package com.llm_thread_analyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;

@Service
public class LLMFeedbackService {

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    @Autowired
    private RestTemplate restTemplate;

    public String gerarFeedbackDidatico(String erroSpotBugs, String codigoFonte) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // O prompt focado no ensino de concorrência
        String prompt = "És um professor universitário especialista em concorrência e threads em Java. " +
                "Um aluno submeteu um código e o analisador estático (SpotBugs) detetou o seguinte problema de concorrência: " + erroSpotBugs + ". " +
                "Explica o erro de forma didática para o aluno, ensinando os conceitos de threads envolvidos. " +
                "Não dês apenas a resposta ou o código corrigido; ajuda-o a raciocinar sobre as condições de corrida (race conditions) ou deadlocks envolvidos.\n\n" +
                "Código do aluno:\n" + codigoFonte;

        // Estrutura do corpo do pedido para a API do OpenRouter
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            System.err.println("Erro ao contactar o OpenRouter: " + e.getMessage());
            return "Ocorreu um problema ao gerar a explicação didática. Por favor, reveja o erro detetado pelo analisador.";
        }
        
        return "Não foi possível obter uma resposta do assistente.";
    }
}