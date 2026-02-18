package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.ConcurrencyIssue;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.config.UserPreferences;
import org.springframework.stereotype.Service;

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

            FindBugs2 findBugs = new FindBugs2();
            findBugs.setProject(project);

            findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            findBugs.setUserPreferences(UserPreferences.createDefaultUserPreferences());

            BugCollectionBugReporter reporter = new BugCollectionBugReporter(project);
            findBugs.setBugReporter(reporter);

            findBugs.execute();

            BugCollection bugCollection = reporter.getBugCollection();
            for(BugInstance bug : bugCollection) {
                if(bug.getBugPattern().getCategory().equals("MT_CORRECTNESS")) {
                    issues.add(new ConcurrencyIssue(bug.getBugPattern().getType(), bug.getPrimarySourceLineAnnotation().getStartLine(), bug.getMessage()));
                }
            }
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
