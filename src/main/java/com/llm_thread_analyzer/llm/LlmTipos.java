package com.llm_thread_analyzer.llm;

import java.util.List;

/**
 * Tipos de dados da camada de LLM, agrupados num só arquivo.
 */
public final class LlmTipos {

    private LlmTipos() {}

    /** Papel de uma mensagem na conversa enviada ao modelo. */
    public enum Papel { USUARIO, MODELO }

    /** Uma mensagem do histórico enviado ao modelo. */
    public record Turno(Papel papel, String texto) {}

    /**
     * Pedido ao modelo: instrução de sistema (persona + regras) e os turnos da conversa.
     * O último turno deve ser do USUARIO.
     */
    public record Requisicao(String instrucaoSistema, List<Turno> turnos) {}

    /** Resposta bem-sucedida de um modelo. */
    public record Resposta(
            String texto,
            String provedor,
            String modelo,
            Integer tokensEntrada,
            Integer tokensSaida,
            String motivoFim,
            String respostaBruta
    ) {}

    /** Motivo de uma tentativa ter falhado. Vai para o registro e para o relatório. */
    public enum MotivoFalha {
        TIMEOUT,          // passou do tempo da chamada
        LIMITE_TAXA,      // 429 — cota ou requisições por minuto
        SERVIDOR,         // 5xx
        BLOQUEADO,        // filtro de segurança do provedor
        RESPOSTA_VAZIA,   // veio sem texto (ex.: MAX_TOKENS sem conteúdo)
        ERRO_CLIENTE,     // 4xx que não é 429 (chave inválida, modelo inexistente...)
        PRAZO_ESGOTADO,   // não havia mais tempo para tentar este modelo
        ERRO_REDE,        // falha de conexão, DNS, etc.
        DESCONHECIDO
    }

    /** Registro de UMA tentativa de chamada (bem-sucedida ou não). */
    public record Tentativa(
            int ordem,
            String nomeConfig,
            String provedor,
            String modelo,
            boolean sucesso,
            MotivoFalha motivoFalha,
            String detalheFalha,
            long latenciaMs,
            Integer tokensEntrada,
            Integer tokensSaida
    ) {}

    /** Resultado final do roteador: a resposta (se houve) e todas as tentativas feitas. */
    public record Resultado(Resposta resposta, List<Tentativa> tentativas, long latenciaTotalMs) {
        public boolean sucesso() { return resposta != null; }
        public boolean usouFallback() {
            return sucesso() && tentativas.stream().anyMatch(t -> !t.sucesso());
        }
    }

    /** Falha de uma chamada, com o motivo classificado. */
    public static class LlmException extends RuntimeException {
        private final MotivoFalha motivo;

        public LlmException(MotivoFalha motivo, String mensagem) {
            super(mensagem);
            this.motivo = motivo;
        }

        public LlmException(MotivoFalha motivo, String mensagem, Throwable causa) {
            super(mensagem, causa);
            this.motivo = motivo;
        }

        public MotivoFalha getMotivo() { return motivo; }

        /** Falhas temporárias que valem nova tentativa no mesmo modelo. */
        public boolean temporaria() {
            return motivo == MotivoFalha.SERVIDOR || motivo == MotivoFalha.TIMEOUT || motivo == MotivoFalha.ERRO_REDE;
        }
    }
}
