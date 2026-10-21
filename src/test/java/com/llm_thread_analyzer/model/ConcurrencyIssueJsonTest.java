package com.llm_thread_analyzer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Trava do formato público do JSON de /analisar: as chaves de um alerta e do resultado não podem mudar.
 */
class ConcurrencyIssueJsonTest {

    // Mesmo construtor que o Spring usa para montar o ObjectMapper dos endpoints.
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @SuppressWarnings("unchecked")
    void chavesDoJsonPublicoSaoPreservadas() throws Exception {
        ConcurrencyIssue issue = new ConcurrencyIssue("AT_NONATOMIC_64BIT_PRIMITIVE", 5, "mensagem");
        CodeAnalysisResults resultado = new CodeAnalysisResults(List.of(issue));

        Map<String, Object> json = mapper.readValue(mapper.writeValueAsString(resultado), Map.class);
        assertEquals(Set.of("issues", "batchError", "batchErrorMessage", "fileName"), json.keySet());

        Map<String, Object> alerta = ((List<Map<String, Object>>) json.get("issues")).get(0);
        assertEquals(Set.of("type", "lineNumber", "message", "interpretation", "llmError", "llmErrorMessage"), alerta.keySet());
    }
}
