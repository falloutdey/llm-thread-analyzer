package com.llm_thread_analyzer.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuração da cadeia de modelos de linguagem (prefixo "llm." no application.properties).
 *
 * Exemplo:
 *   llm.cadeia=gemini-flash,gemini-flash-lite
 *   llm.modelos.gemini-flash.tipo=gemini
 *   llm.modelos.gemini-flash.modelo=gemini-2.5-flash
 *   llm.modelos.gemini-flash.chave=${GEMINI_API_KEY:}
 *
 * A ordem de "cadeia" é a ordem de tentativa. Um modelo sem chave é ignorado (com aviso no log).
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** Nomes dos modelos (chaves de "modelos"), na ordem em que serão tentados. */
    private List<String> cadeia = new ArrayList<>();

    /** Configuração de cada modelo, indexada pelo nome usado em "cadeia". */
    private Map<String, Modelo> modelos = new LinkedHashMap<>();

    /** Se false, só o primeiro modelo da cadeia é usado (útil na Etapa 1, para manter o modelo fixo). */
    private boolean fallbackHabilitado = true;

    /** Tempo máximo para abrir a conexão com o provedor. */
    private long timeoutConexaoMs = 5_000;

    /** Tempo máximo de UMA chamada (do envio ao fim da resposta). */
    private long timeoutChamadaMs = 30_000;

    /** Tempo máximo somando todas as tentativas e esperas de uma mesma resposta do tutor. */
    private long prazoTotalMs = 45_000;

    /** Tentativas no mesmo modelo para falhas temporárias (503, timeout) antes de passar ao próximo. */
    private int tentativasPorModelo = 1;

    public static class Modelo {
        /** "gemini" ou "openai" (qualquer API compatível com /chat/completions: Groq, OpenRouter, etc.). */
        private String tipo = "gemini";
        /** Identificador do modelo no provedor, ex.: gemini-2.5-flash. */
        private String modelo;
        /** URL do endpoint. Para Gemini pode ficar vazia (é montada a partir do modelo). */
        private String url;
        /** Chave da API. Use variável de ambiente; nunca escreva a chave no arquivo. */
        private String chave;
        private double temperatura = 0.3;
        private int maxTokens = 1024;
        /** Intervalo mínimo entre o início de duas chamadas a este modelo (limite de requisições por minuto). */
        private long intervaloMinimoMs = 0;
        /** Só Gemini 2.5: orçamento de "thinking". Vazio = padrão do modelo; 0 = desliga (respostas mais rápidas). */
        private Integer thinkingBudget;
        /** Gemini 3.x em diante: nível de "thinking" (minimal, low, medium, high). Tem prioridade sobre thinkingBudget. */
        private String thinkingLevel;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getModelo() { return modelo; }
        public void setModelo(String modelo) { this.modelo = modelo; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getChave() { return chave; }
        public void setChave(String chave) { this.chave = chave; }
        public double getTemperatura() { return temperatura; }
        public void setTemperatura(double temperatura) { this.temperatura = temperatura; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public long getIntervaloMinimoMs() { return intervaloMinimoMs; }
        public void setIntervaloMinimoMs(long intervaloMinimoMs) { this.intervaloMinimoMs = intervaloMinimoMs; }
        public Integer getThinkingBudget() { return thinkingBudget; }
        public void setThinkingBudget(Integer thinkingBudget) { this.thinkingBudget = thinkingBudget; }
        public String getThinkingLevel() { return thinkingLevel; }
        public void setThinkingLevel(String thinkingLevel) { this.thinkingLevel = thinkingLevel; }
    }

    public List<String> getCadeia() { return cadeia; }
    public void setCadeia(List<String> cadeia) { this.cadeia = cadeia; }
    public Map<String, Modelo> getModelos() { return modelos; }
    public void setModelos(Map<String, Modelo> modelos) { this.modelos = modelos; }
    public boolean isFallbackHabilitado() { return fallbackHabilitado; }
    public void setFallbackHabilitado(boolean fallbackHabilitado) { this.fallbackHabilitado = fallbackHabilitado; }
    public long getTimeoutConexaoMs() { return timeoutConexaoMs; }
    public void setTimeoutConexaoMs(long timeoutConexaoMs) { this.timeoutConexaoMs = timeoutConexaoMs; }
    public long getTimeoutChamadaMs() { return timeoutChamadaMs; }
    public void setTimeoutChamadaMs(long timeoutChamadaMs) { this.timeoutChamadaMs = timeoutChamadaMs; }
    public long getPrazoTotalMs() { return prazoTotalMs; }
    public void setPrazoTotalMs(long prazoTotalMs) { this.prazoTotalMs = prazoTotalMs; }
    public int getTentativasPorModelo() { return tentativasPorModelo; }
    public void setTentativasPorModelo(int tentativasPorModelo) { this.tentativasPorModelo = tentativasPorModelo; }
}
