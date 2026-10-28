package com.llm_thread_analyzer.tutor.entity;

import com.llm_thread_analyzer.tutor.entity.TutorEnums.Modo;
import jakarta.persistence.*;

import java.time.Instant;

/** Cada vez que o aluno clica em "Analisar". Guarda o código exatamente como foi enviado. */
@Entity
@Table(name = "submissao")
public class Submissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String alunoId;

    @Column(length = 64)
    private String sessaoId;

    @Column(length = 255)
    private String arquivo;

    /** 1, 2, 3... por aluno + arquivo. */
    private int numeroTentativa;

    @Column(length = 200_000)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Modo modo;

    private boolean compilou;

    @Column(length = 20_000)
    private String erroCompilacao;

    private int totalAlertas;

    private long duracaoAnaliseMs;

    private Instant criadaEm = Instant.now();

    public Long getId() { return id; }
    public String getAlunoId() { return alunoId; }
    public void setAlunoId(String alunoId) { this.alunoId = alunoId; }
    public String getSessaoId() { return sessaoId; }
    public void setSessaoId(String sessaoId) { this.sessaoId = sessaoId; }
    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }
    public int getNumeroTentativa() { return numeroTentativa; }
    public void setNumeroTentativa(int numeroTentativa) { this.numeroTentativa = numeroTentativa; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Modo getModo() { return modo; }
    public void setModo(Modo modo) { this.modo = modo; }
    public boolean isCompilou() { return compilou; }
    public void setCompilou(boolean compilou) { this.compilou = compilou; }
    public String getErroCompilacao() { return erroCompilacao; }
    public void setErroCompilacao(String erroCompilacao) { this.erroCompilacao = erroCompilacao; }
    public int getTotalAlertas() { return totalAlertas; }
    public void setTotalAlertas(int totalAlertas) { this.totalAlertas = totalAlertas; }
    public long getDuracaoAnaliseMs() { return duracaoAnaliseMs; }
    public void setDuracaoAnaliseMs(long duracaoAnaliseMs) { this.duracaoAnaliseMs = duracaoAnaliseMs; }
    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant criadaEm) { this.criadaEm = criadaEm; }
}
