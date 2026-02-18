package com.llm_thread_analyzer.model;

public class SourceCode {

    private static String path;
    private static String content;

    public static String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public static String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
