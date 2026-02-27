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

        try {
            Project project = new Project();
            project.addFile(classFilePath);
            
            // Adicionar a pasta onde o ficheiro .class está ao Classpath Auxiliar
            File classFile = new File(classFilePath);
            project.addAuxClasspathEntry(classFile.getParent());
            project.addAuxClasspathEntry(System.getProperty("java.class.path"));

            FindBugs2 findBugs = new FindBugs2();
            findBugs.setProject(project);

            findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            
            // Forçar o nível máximo de esforço e ativar a categoria explicitamente
            UserPreferences prefs = UserPreferences.createDefaultUserPreferences();
            prefs.setEffort(UserPreferences.EFFORT_MAX);
            findBugs.setUserPreferences(prefs);

            BugCollectionBugReporter reporter = new BugCollectionBugReporter(project);
            reporter.setPriorityThreshold(Priorities.LOW_PRIORITY); // Capturar todos os avisos
            findBugs.setBugReporter(reporter);

            findBugs.execute();

            BugCollection bugCollection = reporter.getBugCollection();
            
            System.out.println("====== RELATÓRIO DO SPOTBUGS ======");
            System.out.println("Total de bugs encontrados: " + bugCollection.getCollection().size());

            for(BugInstance bug : bugCollection) {
                String category = bug.getBugPattern().getCategory();
                String type = bug.getType();
                System.out.println("Bug Detetado -> Categoria: " + category + " | Tipo: " + type);
                
                // Filtro mais robusto: Apanha MT_CORRECTNESS ou qualquer bug cujo tipo mencione "THREAD" ou "SYNCH"
                if(category.equals("MT_CORRECTNESS") || type.contains("THREAD") || type.contains("SYNCH")) {
                    
                    int linha = -1;
                    if(bug.getPrimarySourceLineAnnotation() != null) {
                        linha = bug.getPrimarySourceLineAnnotation().getStartLine();
                    }
                    
                    issues.add(new ConcurrencyIssue(type, linha, bug.getMessage()));
                }
            }
            System.out.println("===================================");
            
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
        return issues;
    }
}