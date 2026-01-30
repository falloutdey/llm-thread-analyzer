package com.llm_thread_analyzer.service;

import org.springframework.stereotype.Service;

import com.llm_thread_analyzer.model.SourceCode;

@Service
public class CodeAnalysisService {

    public void testarConexao() {
        System.out.println("Caminho recebido: " + SourceCode.getCaminho());
        System.out.println("Conteúdo: " + (SourceCode.getConteudo() != null ? SourceCode.getConteudo() : "Vazio"));
    }

}
