package com.llm_thread_analyzer.model;

public class SourceCode {
    private String fileName;
    private String content;

    // Construtor vazio (obrigatório para o Spring)
    public SourceCode() {}

    public SourceCode(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}