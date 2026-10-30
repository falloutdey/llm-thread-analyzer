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

    public record ArquivoDto(Long id, String nome, String conteudo, Instant criadoEm, Instant atualizadoEm) {
        public static ArquivoDto de(ArquivoJava a) {
            return new ArquivoDto(a.getId(), a.getNome(), a.getConteudo(), a.getCriadoEm(), a.getAtualizadoEm());
        }
    }
}
