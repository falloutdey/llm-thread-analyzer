package com.llm_thread_analyzer.model;

public class ConcurrencyIssue {

    private String type;
    private int lineNumber;
    private String message;
    private String interpretation;

    // Campos para sinalizar falha do LLM de forma estruturada
    // Permite distinguir "Gemini respondeu" de "Gemini falhou" no JSON de validação
    // Sem esses campos, um erro de API apareceria como interpretation válida — contaminando a métrica de acurácia
    private boolean llmError = false;
    private String llmErrorMessage;

    public ConcurrencyIssue() {}

    public ConcurrencyIssue(String type, int lineNumber, String message) {
        this.type = type;
        // lineNumber = 0 significa bug de nível estrutural (classe/método) sem linha específica identificável
        // lineNumber > 0 é uma linha real no código fonte do aluno
        this.lineNumber = lineNumber;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public boolean isLlmError() {
        return llmError;
    }

    public void setLlmError(boolean llmError) {
        this.llmError = llmError;
    }

    public String getLlmErrorMessage() {
        return llmErrorMessage;
    }

    public void setLlmErrorMessage(String llmErrorMessage) {
        this.llmErrorMessage = llmErrorMessage;
    }
}