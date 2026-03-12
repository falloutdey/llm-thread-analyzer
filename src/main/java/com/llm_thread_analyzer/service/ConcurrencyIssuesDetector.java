package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.ConcurrencyIssue;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.config.UserPreferences;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConcurrencyIssuesDetector {

    public List<ConcurrencyIssue> detectConcurrencyIssues(String classFilePath) {
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
        File[] allClasses = classDir.listFiles((dir, name) -> name.endsWith(".class"));
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

                // Filtra apenas bugs de concorrência: categoria MT_CORRECTNESS
                // ou tipos cujo nome mencione THREAD ou SYNCH
                if (category.equals("MT_CORRECTNESS") || type.contains("THREAD") || type.contains("SYNCH")) {

                    // lineNumber 0 indica bug de nível estrutural (classe/método)
                    // sem linha específica identificável no código fonte
                    int linha = 0;
                    if (bug.getPrimarySourceLineAnnotation() != null) {
                        linha = bug.getPrimarySourceLineAnnotation().getStartLine();
                    }

                    issues.add(new ConcurrencyIssue(type, linha, bug.getMessage()));
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
}