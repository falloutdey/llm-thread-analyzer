package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.ConcurrencyIssue;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.config.UserPreferences;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ConcurrencyIssuesDetector {

    public List<ConcurrencyIssue> detectConcurrencyIssues(String classFilePath) {
        return analisar(classFilePath, false, null);
    }

    /**
     * Análise de um projeto com packages: percorre as subpastas das classes e marca em cada alerta
     * o arquivo de origem (ex.: "banco/Conta.java"), conforme o package e o nome do arquivo-fonte.
     *
     * @param arquivosDoProjeto caminhos dos arquivos do projeto; alerta de outro arquivo fica sem nome
     */
    public List<ConcurrencyIssue> detectConcurrencyIssuesProjeto(String classesDir, Set<String> arquivosDoProjeto) {
        return analisar(classesDir, true, arquivosDoProjeto);
    }

    private List<ConcurrencyIssue> analisar(String classFilePath, boolean projeto, Set<String> arquivosDoProjeto) {
        List<ConcurrencyIssue> issues = new ArrayList<>();

        // classFilePath é o diretório temporário gerado pela compilação.
        // Usar o diretório inteiro (em vez de um ficheiro .class específico) permite
        // analisar também classes internas geradas pelo compilador (ex: Main$Worker.class),
        // onde bugs de concorrência em Runnables anónimos frequentemente se manifestam.
        File classDir = new File(classFilePath);

        if (!classDir.exists() || !classDir.isDirectory()) {
            throw new RuntimeException("[SpotBugs] Diretório de classes não encontrado: " + classFilePath);
        }

        // Verifica se o SpotBugs está disponível no classpath antes de prosseguir
        DetectorFactoryCollection detectorFactoryCollection;
        try {
            detectorFactoryCollection = DetectorFactoryCollection.instance();
            if (detectorFactoryCollection == null) {
                throw new RuntimeException("[SpotBugs] DetectorFactoryCollection retornou null. " +
                        "Verifique se o SpotBugs está corretamente no classpath.");
            }
        } catch (Exception e) {
            throw new RuntimeException("[SpotBugs] Falha ao inicializar DetectorFactoryCollection: " + e.getMessage(), e);
        }

        Project project = new Project();

        // Adiciona todos os ficheiros .class do diretório ao projeto SpotBugs
        // (no projeto, também os das subpastas dos packages).
        File[] allClasses = projeto ? classesRecursivas(classDir) : classDir.listFiles((dir, name) -> name.endsWith(".class"));
        if (allClasses != null && allClasses.length > 0) {
            for (File cf : allClasses) {
                project.addFile(cf.getAbsolutePath());
                System.out.println("[SpotBugs] Adicionando ao projeto: " + cf.getName());
            }
        } else {
            throw new RuntimeException("[SpotBugs] Nenhum ficheiro .class encontrado no diretório: " + classDir.getAbsolutePath());
        }

        project.addAuxClasspathEntry(classDir.getAbsolutePath());
        project.addAuxClasspathEntry(System.getProperty("java.class.path"));

        // try-with-resources garante que o FindBugs2 é sempre fechado após a análise,
        // libertando file handles e threads internas — importante em baterias de testes longas
        try (FindBugs2 findBugs = new FindBugs2()) {
            findBugs.setProject(project);
            findBugs.setDetectorFactoryCollection(detectorFactoryCollection);

            // Nível máximo de esforço para cobertura total de bugs
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setEffort(UserPreferences.EFFORT_MAX);
            findBugs.setUserPreferences(prefs);

            // Captura todos os avisos independentemente da prioridade
            BugCollectionBugReporter reporter = new BugCollectionBugReporter(project);
            reporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
            findBugs.setBugReporter(reporter);

            findBugs.execute();

            BugCollection bugCollection = reporter.getBugCollection();

            System.out.println("\n===== RELATÓRIO DO SPOTBUGS =====");
            System.out.println("Total de bugs encontrados (todas as categorias): " + bugCollection.getCollection().size());

            for (BugInstance bug : bugCollection) {
                String category = bug.getBugPattern().getCategory();
                String type = bug.getType();
                System.out.println("Bug Detetado -> Categoria: " + category + " | Tipo: " + type);

                // Filtra bugs de concorrência — categorias e padrões relevantes:
                // MT_CORRECTNESS = categoria principal de thread safety no SpotBugs
                // THREAD, SYNCH, VOLATILE, LOCK = keywords em tipos de bugs de concorrência
                // IS_FIELD_NOT_GUARDED, VO_VOLATILE = detectores específicos de race conditions
                boolean isConcurrencyBug =
                    category.equals("MT_CORRECTNESS") ||
                    type.contains("THREAD") ||
                    type.contains("SYNCH") ||
                    type.contains("VOLATILE") ||
                    type.contains("LOCK") ||
                    type.contains("RACE") ||
                    type.startsWith("IS_") ||
                    type.startsWith("VO_") ||
                    type.startsWith("DL_") ||   // deadlock
                    type.startsWith("LI_") ||   // lock issues
                    type.startsWith("JML_") ||  // inconsistent sync
                    type.startsWith("IS2_") ||  // inconsistent sync v2
                    type.startsWith("MSF_") ||  // mutable servlet field
                    type.startsWith("STCAL_");  // static calendar/date (thread-unsafe)

                if (isConcurrencyBug) {

                    // lineNumber 0 indica bug de nível estrutural (classe/método)
                    // sem linha específica identificável no código fonte
                    int linha = 0;
                    if (bug.getPrimarySourceLineAnnotation() != null) {
                        linha = bug.getPrimarySourceLineAnnotation().getStartLine();
                    }

                    ConcurrencyIssue issue = new ConcurrencyIssue(type, linha, bug.getMessage());
                    if (projeto && bug.getPrimarySourceLineAnnotation() != null) {
                        // getSourcePath() = package em pastas + arquivo-fonte, ex.: "banco/Conta.java"
                        String origem = bug.getPrimarySourceLineAnnotation().getSourcePath();
                        if (origem != null && arquivosDoProjeto != null && arquivosDoProjeto.contains(origem)) {
                            issue.setFileName(origem);
                        }
                    }
                    issues.add(issue);
                }
            }

            System.out.println("Total de issues de concorrência filtrados: " + issues.size());
            System.out.println("=================================\n");

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("[SpotBugs] Erro durante a execução da análise: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("[SpotBugs] Falha na análise: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("[SpotBugs] Falha durante a análise estática: " + e.getMessage(), e);
        }

        return issues;
    }

    private static File[] classesRecursivas(File raiz) {
        try (var caminhos = Files.walk(raiz.toPath())) {
            return caminhos.filter(c -> c.toString().endsWith(".class")).map(Path::toFile).sorted().toArray(File[]::new);
        } catch (IOException e) {
            throw new RuntimeException("[SpotBugs] Falha ao listar as classes: " + e.getMessage(), e);
        }
    }
}
