package com.llm_thread_analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.List;

@Service
public class LLMFeedbackService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Gera feedback didático para um erro de concorrência detetado pelo SpotBugs.
     *
     * Este método lança LlmApiException em caso de falha,
     * em vez de retornar uma string de erro como se fosse uma resposta válida.
     * Isso permite que o CodeAnalysisService marque o campo llmError na issue
     * e mantenha os dados de validação limpos para a métrica de acurácia.
     *
     * @throws LlmApiException se a API do Gemini falhar por qualquer motivo
     */
    public String gerarFeedbackDidatico(String erroSpotBugs, String codigoFonte) {
        System.out.println("\n[LLM] A iniciar pedido de explicação ao Google Gemini...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        // As restrições nos itens 1 e 4 abaixo são o principal mecanismo de contenção de alucinações.
        String prompt = "És um professor universitário especialista em concorrência em Java. " +
                "O analisador estático SpotBugs detetou EXCLUSIVAMENTE o seguinte problema no código do aluno: [" + erroSpotBugs + "]. " +
                "A tua explicação deve:\n" +
                "1. Focar APENAS no problema identificado acima, sem mencionar outros bugs ou vulnerabilidades que possas observar no código.\n" +
                "2. Explicar o conceito de concorrência envolvido nesse problema específico.\n" +
                "3. Orientar o aluno a raciocinar sobre a causa, sem fornecer o código corrigido.\n" +
                "4. Não especular sobre outros possíveis problemas fora do escopo do erro reportado.\n\n" +
                "Código do aluno:\n" + codigoFonte;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                String textoResposta = (String) parts.get(0).get("text");

                System.out.println("[LLM] Resposta recebida do Gemini com sucesso.");
                System.out.println(textoResposta);
                System.out.println();

                return textoResposta;
            } else {
                // Resposta veio mas sem o formato esperado, não é uma resposta válida
                String respostaRaw = responseBody != null ? responseBody.toString() : "null";
                System.err.println("[LLM] Resposta do Gemini fora do formato esperado: " + respostaRaw);
                throw new LlmApiException("Resposta do Gemini fora do formato esperado: " + respostaRaw);
            }

        } catch (HttpClientErrorException e) {
            // Erros 4xx — inclui 429 Too Many Requests (rate limit)
            System.err.println("[LLM] Erro HTTP do cliente ao contactar o Gemini: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new LlmApiException("Erro HTTP " + e.getStatusCode() + " ao contactar o Gemini: " + e.getResponseBodyAsString(), e);

        } catch (HttpServerErrorException e) {
            // Erros 5xx — falha no servidor do Gemini
            System.err.println("[LLM] Erro HTTP do servidor Gemini: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new LlmApiException("Erro no servidor Gemini (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);

        } catch (LlmApiException e) {
            // Re-lançar sem envolver novamente
            throw e;

        } catch (Exception e) {
            System.err.println("[LLM] Erro inesperado ao contactar o Gemini: " + e.getMessage());
            e.printStackTrace();
            throw new LlmApiException("Erro inesperado ao contactar o Gemini: " + e.getMessage(), e);
        }
    }

    /**
     * Exceção customizada para falhas da API do Gemini.
     * Permite que o CodeAnalysisService capture especificamente falhas de LLM
     * e as marque no campo llmError da ConcurrencyIssue, sem interromper
     * o processamento dos outros issues do mesmo arquivo.
     */
    public static class LlmApiException extends RuntimeException {
        public LlmApiException(String message) {
            super(message);
        }

        public LlmApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}