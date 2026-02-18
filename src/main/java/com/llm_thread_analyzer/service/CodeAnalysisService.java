package com.llm_thread_analyzer.service;

import org.springframework.stereotype.Service;

import com.llm_thread_analyzer.model.SourceCode;

@Service
public class CodeAnalysisService {

    public void testarConexao() {
        System.out.println("Caminho recebido: " + SourceCode.getPath());
        System.out.println("Conteúdo: " + (SourceCode.getContent() != null ? SourceCode.getContent() : "Vazio"));
    }

}
