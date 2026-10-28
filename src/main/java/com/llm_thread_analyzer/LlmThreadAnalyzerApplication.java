package com.llm_thread_analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Antes havia exclude = {DataSourceAutoConfiguration.class}, que desligava o banco
// (e, com ele, os repositórios JPA do tutor). O banco agora é usado para registrar as sessões.
@SpringBootApplication
public class LlmThreadAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmThreadAnalyzerApplication.class, args);
    }
}
