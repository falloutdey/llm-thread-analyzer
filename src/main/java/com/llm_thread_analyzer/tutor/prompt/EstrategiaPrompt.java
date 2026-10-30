package com.llm_thread_analyzer.tutor.prompt;

import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Turno;
import com.llm_thread_analyzer.tutor.entity.Conversa;
import com.llm_thread_analyzer.tutor.entity.Mensagem;

import java.util.List;

/**
 * Uma versão do prompt do tutor. Cada versão é um @Component com nome próprio (versao()),
 * escolhida em tutor.versao-prompt. A versão fica gravada em cada conversa e em cada mensagem,
 * então dá para comparar versões no relatório.
 */
public interface EstrategiaPrompt {

    /** Ex.: "v1-explicativo", "v2-socratico". */
    String versao();

    /**
     * Monta o pedido ao modelo.
     *
     * @param conversa  conversa atual (submissão, alerta e modo)
     * @param historico mensagens já gravadas, em ordem (ALUNO e TUTOR; SISTEMA é ignorado)
     */
    Requisicao montar(Conversa conversa, List<Mensagem> historico);

    /**
     * O que o sistema extrai da resposta do modelo. Só "texto" é mostrado ao aluno;
     * o resto é gravado na Mensagem para análise (nulo quando a versão não produz o campo).
     */
    record Interpretacao(String texto, Integer nivelDica, String avaliacaoAluno,
                         String diagnostico, Boolean alunoDescreveuCorrecao) {}

    /**
     * Converte o texto devolvido pelo modelo no que vai para o aluno e para o banco.
     * Padrão: o texto inteiro é a fala, sem campos extras (versão v1).
     */
    default Interpretacao interpretar(String textoModelo, Conversa conversa, List<Mensagem> historico) {
        return new Interpretacao(textoModelo, null, null, null, null);
    }

    /** Texto legível do pedido, para gravar em Mensagem.promptEnviado. */
    static String comoTexto(Requisicao r) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SISTEMA ===\n").append(r.instrucaoSistema()).append("\n");
        for (Turno t : r.turnos()) {
            sb.append("\n=== ").append(t.papel()).append(" ===\n").append(t.texto()).append("\n");
        }
        return sb.toString();
    }
}
