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

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    public String gerarFeedbackDidatico(String erroSpotBugs, String codigoFonte) {
        System.out.println("\n[LLM] A iniciar pedido de explicação ao Google Gemini...");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        String prompt = "És um professor universitário especialista em concorrência e threads em Java. " +
                "Um aluno submeteu um código e o analisador estático detetou o seguinte problema: " + erroSpotBugs + ". " +
                "Explica o erro de forma didática para o aluno, ensinando os conceitos de threads envolvidos. " +
                "Não dês apenas a resposta ou o código corrigido; ajuda-o a raciocinar sobre as condições de corrida ou deadlocks.\n\n" +
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
                
                System.out.println("RESPOSTA RECEBIDA DO GEMINI:");
                System.out.println(textoResposta);
                System.out.println("\n");
                
                return textoResposta;
            } else {
                System.out.println("\n[LLM] A resposta do Gemini não veio no formato esperado: " + responseBody);
            }
        } catch (Exception e) {
            System.err.println("\n[LLM] Erro ao contactar o Gemini: " + e.getMessage());
            e.printStackTrace();
            return "Ocorreu um erro ao gerar a explicação com a IA.";
        }
        
        return "Não foi possível obter resposta do assistente.";
    }
}