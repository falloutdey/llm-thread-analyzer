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
 * Qualquer API no formato OpenAI /v1/chat/completions (Groq, OpenRouter, OpenAI, Ollama local...).
 * Serve como segunda opção de fornecedor, para o fallback não depender só do Google.
 */
public class OpenAiCompatProvider extends LlmProvider {

    public OpenAiCompatProvider(String nomeConfig, LlmProperties.Modelo config, HttpClient http, ObjectMapper json) {
        super(nomeConfig, config, http, json);
    }

    @Override
    public String provedor() { return "openai-compat"; }

    @Override
    public Resposta gerar(Requisicao requisicao, long timeoutMs) {
        if (config.getUrl() == null || config.getUrl().isBlank()) {
            throw new LlmException(MotivoFalha.ERRO_CLIENTE, "Modelo '" + nomeConfig + "' sem llm.modelos." + nomeConfig + ".url");
        }

        List<Map<String, Object>> mensagens = new ArrayList<>();
        if (requisicao.instrucaoSistema() != null && !requisicao.instrucaoSistema().isBlank()) {
            mensagens.add(Map.of("role", "system", "content", requisicao.instrucaoSistema()));
        }
        for (Turno t : requisicao.turnos()) {
            mensagens.add(Map.of("role", t.papel() == Papel.MODELO ? "assistant" : "user", "content", t.texto()));
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", config.getModelo());
        corpo.put("messages", mensagens);
        corpo.put("temperature", config.getTemperatura());
        corpo.put("max_tokens", config.getMaxTokens());
        if (requisicao.esquemaJson() != null) {
            // Nem todo provedor compatível aceita json_schema; json_object é o denominador comum.
            corpo.put("response_format", Map.of("type", "json_object"));
        }

        String bruto = postarJson(config.getUrl(), Map.of("Authorization", "Bearer " + config.getChave()), corpo, timeoutMs);

        JsonNode raiz;
        try {
            raiz = json.readTree(bruto);
        } catch (IOException e) {
            throw new LlmException(MotivoFalha.DESCONHECIDO, "Resposta não é JSON válido", e);
        }

        JsonNode escolha = raiz.path("choices").path(0);
        String texto = escolha.path("message").path("content").asText("");
        String motivoFim = escolha.path("finish_reason").asText(null);
        if (texto.isBlank()) {
            MotivoFalha motivo = "content_filter".equals(motivoFim) ? MotivoFalha.BLOQUEADO : MotivoFalha.RESPOSTA_VAZIA;
            throw new LlmException(motivo, "Texto vazio. finish_reason=" + motivoFim);
        }

        JsonNode uso = raiz.path("usage");
        Integer entrada = uso.has("prompt_tokens") ? uso.get("prompt_tokens").asInt() : null;
        Integer saida = uso.has("completion_tokens") ? uso.get("completion_tokens").asInt() : null;

        return new Resposta(texto.trim(), provedor(), modelo(), entrada, saida, motivoFim, bruto);
    }
}
