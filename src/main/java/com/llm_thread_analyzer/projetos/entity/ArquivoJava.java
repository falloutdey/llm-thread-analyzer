package com.llm_thread_analyzer.projetos.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** Arquivo .java de um projeto. O conteúdo fica no banco (não no disco). */
@Entity
@Table(name = "arquivo_java",
        uniqueConstraints = @UniqueConstraint(name = "uk_arquivo_java_projeto_nome", columnNames = {"projeto_id", "nome"}))
public class ArquivoJava {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private ProjetoJava projeto;

    @Column(nullable = false, length = 100)
    private String nome;

    /** Limite real definido em java.projetos.max-caracteres-arquivo; a coluna tem folga. */
    @Column(nullable = false, length = 200_000)
    private String conteudo = "";

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected ArquivoJava() {}

    public ArquivoJava(ProjetoJava projeto, String nome, String conteudo) {
        this.projeto = projeto;
        this.nome = nome;
        this.conteudo = conteudo;
    }

    public void tocar() { this.atualizadoEm = Instant.now(); }

    public Long getId() { return id; }
    public ProjetoJava getProjeto() { return projeto; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
