package com.llm_thread_analyzer.model;

public class ConcurrencyIssue {

    private String type;
    private int lineNumber;
    private String message;
    private String interpretation;

    public ConcurrencyIssue(String type, int lineNumber, String message) {
        this.type = type;
        this.message = message;
        this.lineNumber = lineNumber;
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
}
