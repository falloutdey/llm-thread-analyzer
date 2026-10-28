package com.llm_thread_analyzer.tutor.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Um problema apontado pela análise estática numa submissão. */
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Submissao submissao;

    /** Código do detector, ex.: IS2_INCONSISTENT_SYNC ou SRC_READ_WITHOUT_JOIN. */
    @Column(length = 120)
    private String tipo;

    /** SPOTBUGS ou FONTE (regras do SourceConcurrencyDetector, prefixo SRC_). */
    @Column(length = 20)
    private String origem;

    private int linha;

    @Column(length = 4_000)
    private String mensagem;

    /** true quando a submissão seguinte do mesmo arquivo não tem mais alerta deste tipo. */
    private boolean resolvido;

    private Long resolvidoNaSubmissaoId;

    private Instant resolvidoEm;

    public Long getId() { return id; }
    public Submissao getSubmissao() { return submissao; }
    public void setSubmissao(Submissao submissao) { this.submissao = submissao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public int getLinha() { return linha; }
    public void setLinha(int linha) { this.linha = linha; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public boolean isResolvido() { return resolvido; }
    public void setResolvido(boolean resolvido) { this.resolvido = resolvido; }
    public Long getResolvidoNaSubmissaoId() { return resolvidoNaSubmissaoId; }
    public void setResolvidoNaSubmissaoId(Long resolvidoNaSubmissaoId) { this.resolvidoNaSubmissaoId = resolvidoNaSubmissaoId; }
    public Instant getResolvidoEm() { return resolvidoEm; }
    public void setResolvidoEm(Instant resolvidoEm) { this.resolvidoEm = resolvidoEm; }
}
