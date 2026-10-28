package com.llm_thread_analyzer.tutor.entity;

public final class TutorEnums {

    private TutorEnums() {}

    /**
     * HIBRIDO: o tutor recebe o relatório da análise estática (SpotBugs + regras no fonte).
     * LLM_ISOLADO: o tutor recebe só o código (condição de comparação).
     */
    public enum Modo { HIBRIDO, LLM_ISOLADO }

    public enum Autor { ALUNO, TUTOR, SISTEMA }

    public enum EstadoConversa { ABERTA, RESOLVIDA, ENCERRADA_PELO_ALUNO, LIMITE_ATINGIDO }

    /** O que a mensagem representa dentro da conversa. */
    public enum TipoMensagem {
        ABERTURA,        // primeira fala do tutor sobre o alerta
        PERGUNTA,        // aluno escreveu livremente
        PEDIDO_DICA,     // aluno clicou em "quero uma dica"
        RESPOSTA,        // tutor respondendo ao aluno
        FALHA_LLM,       // nenhum modelo respondeu dentro do prazo
        AVISO            // mensagem automática do sistema (ex.: alerta resolvido)
    }
}
