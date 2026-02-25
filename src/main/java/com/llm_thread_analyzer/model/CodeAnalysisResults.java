package com.llm_thread_analyzer.model;

import java.util.ArrayList;
import java.util.List;

public class CodeAnalysisResults {

    private List<ConcurrencyIssue> issues = new ArrayList<>();

    public CodeAnalysisResults() {}

    public CodeAnalysisResults(List<ConcurrencyIssue> issues) {
        this.issues = issues;
    }

    public List<ConcurrencyIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ConcurrencyIssue> issues) {
        this.issues = issues;
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }
}