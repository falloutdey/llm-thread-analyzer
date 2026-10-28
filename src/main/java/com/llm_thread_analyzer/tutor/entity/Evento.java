package com.llm_thread_analyzer.tutor.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registro livre de ações do aluno na tela (abriu o editor, abriu um alerta, fechou o chat,
 * copiou texto, etc.). Enviado pelo frontend; serve para reconstruir a sessão depois.
 */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String alunoId;

    @Column(length = 64)
    private String sessaoId;

    @Column(length = 60)
    private String tipo;

    @Column(length = 4_000)
    private String detalhe;

    private Long submissaoId;
    private Long conversaId;

    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }
    public String getAlunoId() { return alunoId; }
    public void setAlunoId(String alunoId) { this.alunoId = alunoId; }
    public String getSessaoId() { return sessaoId; }
    public void setSessaoId(String sessaoId) { this.sessaoId = sessaoId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public Long getSubmissaoId() { return submissaoId; }
    public void setSubmissaoId(Long submissaoId) { this.submissaoId = submissaoId; }
    public Long getConversaId() { return conversaId; }
    public void setConversaId(Long conversaId) { this.conversaId = conversaId; }
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
