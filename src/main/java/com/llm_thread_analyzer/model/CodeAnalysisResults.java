package com.llm_thread_analyzer.model;

import java.util.ArrayList;

public class CodeAnalysisResults {

    private List<ConcurrencyIssue> issues = new ArrayList<>();

    public boolean hasIssues() {
        return !issues.isEmpty();
    }
}
