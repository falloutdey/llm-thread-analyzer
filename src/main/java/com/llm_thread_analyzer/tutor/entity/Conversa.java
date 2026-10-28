package com.llm_thread_analyzer.tutor.entity;

import com.llm_thread_analyzer.tutor.entity.TutorEnums.EstadoConversa;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.Modo;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Diálogo do aluno com o tutor. No modo HIBRIDO é sempre sobre um alerta;
 * no LLM_ISOLADO é sobre a submissão inteira (alerta fica nulo).
 */
@Entity
@Table(name = "conversa")
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Submissao submissao;

    @ManyToOne
    private Alerta alerta;

    @Column(length = 64, nullable = false)
    private String alunoId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Modo modo;

    /** Versão do prompt usada em toda a conversa (ex.: v1-explicativo, v2-socratico). */
    @Column(length = 40)
    private String versaoPrompt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoConversa estado = EstadoConversa.ABERTA;

    private Instant iniciadaEm = Instant.now();

    private Instant encerradaEm;

    public Long getId() { return id; }
    public Submissao getSubmissao() { return submissao; }
    public void setSubmissao(Submissao submissao) { this.submissao = submissao; }
    public Alerta getAlerta() { return alerta; }
    public void setAlerta(Alerta alerta) { this.alerta = alerta; }
    public String getAlunoId() { return alunoId; }
    public void setAlunoId(String alunoId) { this.alunoId = alunoId; }
    public Modo getModo() { return modo; }
    public void setModo(Modo modo) { this.modo = modo; }
    public String getVersaoPrompt() { return versaoPrompt; }
    public void setVersaoPrompt(String versaoPrompt) { this.versaoPrompt = versaoPrompt; }
    public EstadoConversa getEstado() { return estado; }
    public void setEstado(EstadoConversa estado) { this.estado = estado; }
    public Instant getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(Instant iniciadaEm) { this.iniciadaEm = iniciadaEm; }
    public Instant getEncerradaEm() { return encerradaEm; }
    public void setEncerradaEm(Instant encerradaEm) { this.encerradaEm = encerradaEm; }
}
