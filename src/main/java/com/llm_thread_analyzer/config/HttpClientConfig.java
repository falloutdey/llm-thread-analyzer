package com.llm_thread_analyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    /**
     * RestTemplate usado pelo LLMFeedbackService (fluxo antigo /api/files/analisar e bateria).
     * Antes não tinha limite de tempo: uma chamada travada podia segurar a análise para sempre.
     */
    @Bean
    public RestTemplate restTemplate(
            @Value("${llm.timeout-conexao-ms:5000}") int timeoutConexaoMs,
            @Value("${llm.timeout-leitura-legado-ms:60000}") int timeoutLeituraMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutConexaoMs);
        factory.setReadTimeout(timeoutLeituraMs);
        return new RestTemplate(factory);
    }
}
