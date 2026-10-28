package com.llm_thread_analyzer.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llm_thread_analyzer.llm.LlmTipos.LlmException;
import com.llm_thread_analyzer.llm.LlmTipos.MotivoFalha;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resposta;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Um modelo de linguagem configurado (provedor + modelo + chave).
 * As subclasses só montam o corpo do pedido e leem a resposta; o envio HTTP,
 * o limite de tempo e a classificação de erros ficam aqui.
 */
public abstract class LlmProvider {

    protected final String nomeConfig;
    protected final LlmProperties.Modelo config;
    protected final HttpClient http;
    protected final ObjectMapper json;

    protected LlmProvider(String nomeConfig, LlmProperties.Modelo config, HttpClient http, ObjectMapper json) {
        this.nomeConfig = nomeConfig;
        this.config = config;
        this.http = http;
        this.json = json;
    }

    /** Nome usado em llm.cadeia (ex.: "gemini-flash"). */
    public String nomeConfig() { return nomeConfig; }

    /** Nome do provedor para o registro (ex.: "gemini", "openai"). */
    public abstract String provedor();

    /** Identificador do modelo no provedor (ex.: "gemini-2.5-flash"). */
    public String modelo() { return config.getModelo(); }

    public LlmProperties.Modelo config() { return config; }

    /** Gera uma resposta, respeitando o tempo máximo informado. */
    public abstract Resposta gerar(Requisicao requisicao, long timeoutMs);

    /**
     * Envia um POST JSON e devolve o corpo da resposta (status 2xx).
     * O limite de tempo cobre a chamada inteira, incluindo a leitura do corpo.
     */
    protected String postarJson(String url, Map<String, String> cabecalhos, Object corpo, long timeoutMs) {
        String corpoJson;
        try {
            corpoJson = json.writeValueAsString(corpo);
        } catch (IOException e) {
            throw new LlmException(MotivoFalha.DESCONHECIDO, "Falha ao montar o JSON do pedido: " + e.getMessage(), e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpoJson));
        cabecalhos.forEach(builder::header);

        CompletableFuture<HttpResponse<String>> futuro =
                http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> resposta;
        try {
            resposta = futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new LlmException(MotivoFalha.TIMEOUT, "Sem resposta em " + timeoutMs + " ms", e);
        } catch (InterruptedException e) {
            futuro.cancel(true);
            Thread.currentThread().interrupt();
            throw new LlmException(MotivoFalha.DESCONHECIDO, "Chamada interrompida", e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof HttpTimeoutException) {
                throw new LlmException(MotivoFalha.TIMEOUT, "Timeout HTTP: " + causa.getMessage(), causa);
            }
            throw new LlmException(MotivoFalha.ERRO_REDE, "Falha de rede: " + causa, causa);
        }

        int status = resposta.statusCode();
        if (status >= 200 && status < 300) {
            return resposta.body();
        }
        String trecho = resumir(resposta.body(), 500);
        if (status == 429) {
            throw new LlmException(MotivoFalha.LIMITE_TAXA, "HTTP 429: " + trecho);
        }
        if (status >= 500) {
            throw new LlmException(MotivoFalha.SERVIDOR, "HTTP " + status + ": " + trecho);
        }
        throw new LlmException(MotivoFalha.ERRO_CLIENTE, "HTTP " + status + ": " + trecho);
    }

    protected static String resumir(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max) + "...";
    }

    protected static Integer inteiroOuNulo(Object valor) {
        return valor instanceof Number n ? n.intValue() : null;
    }
}
