package com.llm_thread_analyzer.projetos.dto;

import com.llm_thread_analyzer.projetos.entity.ArquivoJava;
import com.llm_thread_analyzer.projetos.entity.ProjetoJava;

import java.time.Instant;
import java.util.List;

/** Corpos de requisição e resposta da API /api/java/projetos. */
public final class ProjetosDtos {

    private ProjetosDtos() {}

    // ---------- Requisições ----------

    public record NovoProjeto(String nome) {}

    public record RenomearProjeto(String nome) {}

    /** conteudo vazio ou ausente: o arquivo começa com uma classe com main(). */
    public record NovoArquivo(String nome, String conteudo) {}

    /** Campos ausentes não são alterados. */
    public record AlterarArquivo(String nome, String conteudo) {}

    /** arquivoAtivoId: arquivo aberto no editor (vai para o registro); sessaoId: sessão do editor. */
    public record PedidoAnalise(Long arquivoAtivoId, String sessaoId) {}

    /**
     * Mesmos campos da análise: roda o main do arquivo aberto (ou o único main do projeto).
     * repeticoes: ausente = 1 (botão Executar); 5 = "Executar 5 vezes".
     */
    public record PedidoExecucao(Long arquivoAtivoId, String sessaoId, Integer repeticoes) {}

    // ---------- Respostas ----------

    public record ProjetoResumo(Long id, String nome, Instant criadoEm, Instant atualizadoEm, long quantidadeArquivos) {
        public static ProjetoResumo de(ProjetoJava p, long quantidadeArquivos) {
            return new ProjetoResumo(p.getId(), p.getNome(), p.getCriadoEm(), p.getAtualizadoEm(), quantidadeArquivos);
        }
    }

    public record ProjetoDetalhe(Long id, String nome, Instant criadoEm, Instant atualizadoEm, List<ArquivoDto> arquivos) {
        public static ProjetoDetalhe de(ProjetoJava p, List<ArquivoJava> arquivos) {
            return new ProjetoDetalhe(p.getId(), p.getNome(), p.getCriadoEm(), p.getAtualizadoEm(),
                    arquivos.stream().map(ArquivoDto::de).toList());
        }
    }

    /**
     * Resultado do botão Executar (um clique).
     * compilou=false: erroCompilacao tem a mensagem do javac, o programa não rodou e rodadas vem vazia.
     * rodadas: uma por execução, na ordem. Param na primeira interrompida por tempo, então podem
     *          ser menos que repeticoesPedidas.
     * numeroExecucao: número deste clique no projeto (1, 2, 3...).
     */
    public record ExecucaoResponse(String grupo, int numeroExecucao, boolean compilou, String erroCompilacao,
                                   String arquivoExecutado, String classeExecutada, int limiteSegundos,
                                   int repeticoesPedidas, List<RodadaDto> rodadas) {}

    /**
     * Uma execução do programa.
     * interrupcao: "TEMPO" (passou de limiteSegundos) ou "SAIDA" (saída grande demais); null = terminou sozinho.
     * codigoSaida: 0 = terminou normalmente; outro valor = terminou com erro (ex.: exceção não tratada);
     *              null quando foi interrompida.
     * aviso: explicação quando o erro vem das restrições da execução (arquivos, rede, teclado).
     */
    public record RodadaDto(int ordem, String saida, boolean saidaCortada, Integer codigoSaida, long duracaoMs,
                            String interrupcao, String aviso) {}

    public record ArquivoDto(Long id, String nome, String conteudo, Instant criadoEm, Instant atualizadoEm) {
        public static ArquivoDto de(ArquivoJava a) {
            return new ArquivoDto(a.getId(), a.getNome(), a.getConteudo(), a.getCriadoEm(), a.getAtualizadoEm());
        }
    }
}
