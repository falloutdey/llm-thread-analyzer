package com.llm_thread_analyzer.tutor.dto;

import java.time.Instant;
import java.util.List;

/** Objetos de entrada e saída da API do tutor (JSON). */
public final class TutorDtos {

    private TutorDtos() {}

    // ---------- Entrada ----------

    /** modo: "HIBRIDO" (padrão) ou "LLM_ISOLADO". */
    public record NovaSubmissaoRequest(String alunoId, String sessaoId, String fileName, String content, String modo) {}

    /** alertaId é obrigatório no modo HIBRIDO e ignorado no LLM_ISOLADO. */
    public record NovaConversaRequest(String alunoId, Long submissaoId, Long alertaId) {}

    /** tipo: "PERGUNTA" (padrão) ou "PEDIDO_DICA". */
    public record NovaMensagemRequest(String alunoId, String texto, String tipo) {}

    public record AlunoRequest(String alunoId) {}

    public record EventoRequest(String alunoId, String sessaoId, String tipo, String detalhe, Long submissaoId, Long conversaId) {}

    // ---------- Saída ----------

    public record AlertaDto(Long id, String tipo, String origem, int linha, String mensagem) {}

    public record SubmissaoResponse(
            Long submissaoId,
            int numeroTentativa,
            String modo,
            boolean compilou,
            String erroCompilacao,
            List<AlertaDto> alertas,
            /** Tipos de alerta da submissão anterior que não aparecem mais nesta. */
            List<String> resolvidosDesdeAnterior
    ) {}

    public record MensagemDto(Long id, int ordem, String autor, String tipo, String texto, Instant criadaEm, String modelo) {}

    public record ConversaResponse(
            Long conversaId,
            Long submissaoId,
            Long alertaId,
            String modo,
            String estado,
            String versaoPrompt,
            List<MensagemDto> mensagens,
            /** true quando a última fala é uma falha do LLM (o frontend pode oferecer "tentar novamente"). */
            boolean falhaLlm,
            int mensagensRestantes
    ) {}
}
