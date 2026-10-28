package com.llm_thread_analyzer.tutor;

import org.springframework.http.HttpStatus;

/** Erro de uso da API do tutor, com o status HTTP que deve ser devolvido. */
public class TutorException extends RuntimeException {

    private final HttpStatus status;

    public TutorException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
