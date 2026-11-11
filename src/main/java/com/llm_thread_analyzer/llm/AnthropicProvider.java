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
 * Modelos Claude, pela Messages API da Anthropic (POST /v1/messages).
 *
 * O que acontece em cada chamada, na ordem:
 *   1. O prompt do sistema vai no campo "system"; os turnos, em "messages" (user/assistant),
 *      juntando turnos seguidos do mesmo papel e garantindo que a conversa comece pelo aluno.
 *   2. Quando o tutor pede JSON, o esquema vai em output_config.format (structured outputs),
 *      adaptado ao que a API aceita (EsquemaEstrito: sem minimum/maximum, additionalProperties=false).
 *   3. thinking e effort vêm da configuração (llm.modelos.X.pensar e .esforco).
 *      Temperatura não é enviada: os modelos 5.5 não aceitam.
 *   4. A resposta é o texto dos blocos "text". Recusa do modelo vira BLOQUEADO;
 *      resposta cortada por max_tokens vira RESPOSTA_VAZIA, e o roteador tenta o próximo modelo.
 */
public class AnthropicProvider extends LlmProvider {

    static final String URL_PADRAO = "https://api.anthropic.com/v1/messages";
    static final String VERSAO_API = "2023-06-01";

    public AnthropicProvider(String nomeConfig, LlmProperties.Modelo config, HttpClient http, ObjectMapper json) {
        super(nomeConfig, config, http, json);
    }

    @Override
    public String provedor() { return "anthropic"; }

    @Override
    public Resposta gerar(Requisicao requisicao, long timeoutMs) {
        String url = config.getUrl() == null || config.getUrl().isBlank() ? URL_PADRAO : config.getUrl();
        Map<String, Object> corpo = montarCorpo(requisicao);

        String bruto = postarJson(url, Map.of("x-api-key", config.getChave(), "anthropic-version", VERSAO_API),
                corpo, timeoutMs);

        JsonNode raiz;
        try {
            raiz = json.readTree(bruto);
        } catch (IOException e) {
            throw new LlmException(MotivoFalha.DESCONHECIDO, "Resposta não é JSON válido", e);
        }

        StringBuilder texto = new StringBuilder();
        for (JsonNode bloco : raiz.path("content")) {
            if ("text".equals(bloco.path("type").asText())) texto.append(bloco.path("text").asText(""));
        }
        String motivoFim = raiz.path("stop_reason").asText(null);
        if ("refusal".equals(motivoFim)) {
            throw new LlmException(MotivoFalha.BLOQUEADO, "O modelo recusou a resposta. stop_reason=refusal");
        }
        if ("max_tokens".equals(motivoFim)) {
            throw new LlmException(MotivoFalha.RESPOSTA_VAZIA, "Resposta cortada pelo limite de tokens. stop_reason=max_tokens");
        }
        if (texto.toString().isBlank()) {
            throw new LlmException(MotivoFalha.RESPOSTA_VAZIA, "Texto vazio. stop_reason=" + motivoFim);
        }

        JsonNode uso = raiz.path("usage");
        Integer entrada = uso.has("input_tokens") ? uso.get("input_tokens").asInt() : null;
        Integer saida = uso.has("output_tokens") ? uso.get("output_tokens").asInt() : null;
        return new Resposta(texto.toString().trim(), provedor(), modelo(), entrada, saida, motivoFim, bruto);
    }

    Map<String, Object> montarCorpo(Requisicao requisicao) {
        List<Map<String, Object>> mensagens = new ArrayList<>();
        for (Turno t : requisicao.turnos()) {
            String papel = t.papel() == Papel.MODELO ? "assistant" : "user";
            if (mensagens.isEmpty() && papel.equals("assistant")) {
                mensagens.add(new LinkedHashMap<>(Map.of("role", "user", "content", "Início da conversa.")));
            }
            Map<String, Object> ultima = mensagens.isEmpty() ? null : mensagens.get(mensagens.size() - 1);
            if (ultima != null && papel.equals(ultima.get("role"))) {
                ultima.put("content", ultima.get("content") + "\n\n" + t.texto());
            } else {
                mensagens.add(new LinkedHashMap<>(Map.of("role", papel, "content", t.texto())));
            }
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", config.getModelo());
        corpo.put("max_tokens", config.getMaxTokens());
        if (requisicao.instrucaoSistema() != null && !requisicao.instrucaoSistema().isBlank()) {
            corpo.put("system", requisicao.instrucaoSistema());
        }
        corpo.put("messages", mensagens);

        if (Boolean.FALSE.equals(config.getPensar())) {
            corpo.put("thinking", Map.of("type", "disabled"));
        } else if (Boolean.TRUE.equals(config.getPensar())) {
            corpo.put("thinking", Map.of("type", "adaptive"));
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        if (config.getEsforco() != null && !config.getEsforco().isBlank()) {
            saida.put("effort", config.getEsforco());
        }
        if (requisicao.esquemaJson() != null) {
            saida.put("format", Map.of("type", "json_schema", "schema", EsquemaEstrito.adaptar(requisicao.esquemaJson())));
        }
        if (!saida.isEmpty()) corpo.put("output_config", saida);
        return corpo;
    }
}
