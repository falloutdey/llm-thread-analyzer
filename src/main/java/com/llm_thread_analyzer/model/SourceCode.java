package com.llm_thread_analyzer.model;

public class SourceCode {

    private static String caminho;
    private static String conteudo;

    public static String getCaminho() {
        return caminho;
    }

    public void setCaminho(String caminho) {
        this.caminho = caminho;
    }

    public static String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

}
