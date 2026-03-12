package com.llm_thread_analyzer.model;

import java.util.ArrayList;
import java.util.List;

public class CodeAnalysisResults {

    private List<ConcurrencyIssue> issues = new ArrayList<>();

    // Campos para identificar falhas no processamento batch.
    // Permite que um ficheiro com erro não quebre o JSON inteiro da bateria JCB —
    // o resultado desse ficheiro vem marcado com batchError: true e os restantes continuam.
    private boolean batchError = false;
    private String batchErrorMessage;
    private String fileName;

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

    public boolean isBatchError() { 
        return batchError; 
    }

    public void setBatchError(boolean batchError) { 
        this.batchError = batchError; 
    }

    public String getBatchErrorMessage() { 
        return batchErrorMessage; 
    }

    public void setBatchErrorMessage(String batchErrorMessage) { 
        this.batchErrorMessage = batchErrorMessage; 
    }

    public String getFileName() { 
        return fileName; 
    }

    public void setFileName(String fileName) { 
        this.fileName = fileName; 
    }
}