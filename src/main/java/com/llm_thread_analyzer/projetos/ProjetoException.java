package com.llm_thread_analyzer.projetos;

import org.springframework.http.HttpStatus;

/** Erro de uso da API de projetos Java, com o status HTTP que deve ser devolvido. */
public class ProjetoException extends RuntimeException {

    private final HttpStatus status;

    public ProjetoException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
