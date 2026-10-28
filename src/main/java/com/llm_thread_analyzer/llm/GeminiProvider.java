package com.llm_thread_analyzer.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmTipos.LlmException;
import com.llm_thread_analyzer.llm.LlmTipos.MotivoFalha;
import com.llm_thread_analyzer.llm.LlmTipos.Papel;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resposta;
import com.llm_thread_analyzer.llm.LlmTipos.Turno;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini (API generateContent). Mantém as mesmas opções do LLMFeedbackService original:
 * safetySettings BLOCK_NONE, temperatura e maxOutputTokens configuráveis.
 */
public class GeminiProvider extends LlmProvider {

    private static final String URL_PADRAO =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    public GeminiProvider(String nomeConfig, LlmProperties.Modelo config, HttpClient http, ObjectMapper json) {
        super(nomeConfig, config, http, json);
    }

    @Override
    public String provedor() { return "gemini"; }

    @Override
    public Resposta gerar(Requisicao requisicao, long timeoutMs) {
        String url = (config.getUrl() == null || config.getUrl().isBlank())
                ? String.format(URL_PADRAO, config.getModelo())
                : config.getUrl();

        Map<String, Object> corpo = new LinkedHashMap<>();
        if (requisicao.instrucaoSistema() != null && !requisicao.instrucaoSistema().isBlank()) {
            corpo.put("systemInstruction", Map.of("parts", List.of(Map.of("text", requisicao.instrucaoSistema()))));
        }

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Turno t : requisicao.turnos()) {
            contents.add(Map.of(
                    "role", t.papel() == Papel.MODELO ? "model" : "user",
                    "parts", List.of(Map.of("text", t.texto()))
            ));
        }
        corpo.put("contents", contents);

        corpo.put("safetySettings", List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE")
        ));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", config.getTemperatura());
        generationConfig.put("maxOutputTokens", config.getMaxTokens());
        generationConfig.put("topP", 0.8);
        // Série 2.5 usa thinkingBudget; série 3 em diante usa thinkingLevel. Nunca os dois juntos (a API recusa).
        if (config.getThinkingLevel() != null && !config.getThinkingLevel().isBlank()) {
            generationConfig.put("thinkingConfig", Map.of("thinkingLevel", config.getThinkingLevel().trim()));
        } else if (config.getThinkingBudget() != null) {
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", config.getThinkingBudget()));
        }
        corpo.put("generationConfig", generationConfig);

        String bruto = postarJson(url, Map.of("x-goog-api-key", config.getChave()), corpo, timeoutMs);
        return interpretar(bruto);
    }

    private Resposta interpretar(String bruto) {
        JsonNode raiz;
        try {
            raiz = json.readTree(bruto);
        } catch (IOException e) {
            throw new LlmException(MotivoFalha.DESCONHECIDO, "Resposta do Gemini não é JSON válido", e);
        }

        JsonNode candidatos = raiz.path("candidates");
        if (!candidatos.isArray() || candidatos.isEmpty()) {
            String feedback = raiz.path("promptFeedback").toString();
            throw new LlmException(MotivoFalha.BLOQUEADO, "Sem candidatos (bloqueio?): " + resumir(feedback, 300));
        }

        JsonNode candidato = candidatos.get(0);
        String motivoFim = candidato.path("finishReason").asText(null);

        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : candidato.path("content").path("parts")) {
            // Partes de "pensamento" (thought=true) não são mostradas ao aluno.
            if (parte.path("thought").asBoolean(false)) continue;
            texto.append(parte.path("text").asText(""));
        }

        if (texto.toString().isBlank()) {
            MotivoFalha motivo = "SAFETY".equals(motivoFim) ? MotivoFalha.BLOQUEADO : MotivoFalha.RESPOSTA_VAZIA;
            throw new LlmException(motivo, "Texto vazio. finishReason=" + motivoFim);
        }

        JsonNode uso = raiz.path("usageMetadata");
        Integer entrada = uso.has("promptTokenCount") ? uso.get("promptTokenCount").asInt() : null;
        Integer saida = uso.has("candidatesTokenCount") ? uso.get("candidatesTokenCount").asInt() : null;

        return new Resposta(texto.toString().trim(), provedor(), modelo(), entrada, saida, motivoFim, bruto);
    }
}
