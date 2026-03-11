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

        File classFile = new File(classFilePath);
        File classDir = classFile.getParentFile();

        // CORREÇÃO 1: Verificar se o ficheiro .class existe antes de prosseguir
        if (!classFile.exists()) {
            throw new RuntimeException("Ficheiro .class não encontrado no caminho: " + classFilePath);
        }

        // Verificar se a DetectorFactoryCollection está disponível (pode falhar em ambientes headless/Docker)
        DetectorFactoryCollection detectorFactoryCollection;
        try {
            detectorFactoryCollection = DetectorFactoryCollection.instance();
            if (detectorFactoryCollection == null) {
                throw new RuntimeException("[SpotBugs] DetectorFactoryCollection retornou null. " +
                        "Verifique se o SpotBugs está corretamente no classpath.");
            }
        } catch (Exception e) {
            throw new RuntimeException("[SpotBugs] FALHA CRÍTICA ao inicializar DetectorFactoryCollection: " + e.getMessage(), e);
        }

        Project project = new Project();

        // CORREÇÃO 2: Adicionar TODOS os .class do diretório temporário ao projeto
        // Necessário para capturar bugs em classes internas (ex: Main$Worker.class, Main$1.class)
        // onde os bugs de concorrência frequentemente se manifestam (Runnables anônimos, etc.)
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

        // CORREÇÃO 3: Usar try-with-resources para garantir que o FindBugs2 é sempre fechado
        // Evita vazamento de file handles e threads internas acumuladas em baterias de testes longas
        try (FindBugs2 findBugs = new FindBugs2()) {
            findBugs.setProject(project);
            findBugs.setDetectorFactoryCollection(detectorFactoryCollection);

            // Forçar o nível máximo de esforço para cobertura total
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setEffort(UserPreferences.EFFORT_MAX);
            findBugs.setUserPreferences(prefs);

            BugCollectionBugReporter reporter = new BugCollectionBugReporter(project);
            reporter.setPriorityThreshold(Priorities.LOW_PRIORITY); // Capturar todos os avisos
            findBugs.setBugReporter(reporter);

            findBugs.execute();

            BugCollection bugCollection = reporter.getBugCollection();

            System.out.println("\n===== RELATÓRIO DO SPOTBUGS =====");
            System.out.println("Total de bugs encontrados (todas as categorias): " + bugCollection.getCollection().size());

            for (BugInstance bug : bugCollection) {
                String category = bug.getBugPattern().getCategory();
                String type = bug.getType();
                System.out.println("Bug Detetado -> Categoria: " + category + " | Tipo: " + type);

                // Filtro robusto: MT_CORRECTNESS ou qualquer tipo que mencione THREAD ou SYNCH
                if (category.equals("MT_CORRECTNESS") || type.contains("THREAD") || type.contains("SYNCH")) {

                    // CORREÇÃO 4: lineNumber 0 em vez de -1 para bugs sem linha determinável
                    // 0 = bug de nível estrutural (classe/método) sem linha específica identificável
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
            // CORREÇÃO 5: Lançar exceção em vez de engolir o erro e retornar lista vazia silenciosamente
            // Retornar lista vazia faria o pipeline parecer "sem bugs" quando na verdade o SpotBugs falhou
            throw new RuntimeException("[SpotBugs] Erro durante a execução da análise: " + e.getMessage(), e);
        } catch (Exception e) {
            // CORREÇÃO 5 (cont.): Mesmo para exceções genéricas — nunca retornar silenciosamente
            System.err.println("[SpotBugs] FALHA CRÍTICA NA ANÁLISE: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("[SpotBugs] Falha crítica durante a análise estática: " + e.getMessage(), e);
        }

        return issues;
    }
}