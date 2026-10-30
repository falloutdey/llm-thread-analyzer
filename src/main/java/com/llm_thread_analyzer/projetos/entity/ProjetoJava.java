package com.llm_thread_analyzer.projetos.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Projeto Java de um aluno. O aluno é identificado pelo idestudante do login do Sumawma;
 * não há chave estrangeira para o banco do Sumawma (são bancos separados).
 */
@Entity
@Table(name = "projeto_java", indexes = @Index(name = "idx_projeto_java_estudante", columnList = "id_estudante"))
public class ProjetoJava {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_estudante", nullable = false)
    private Long idEstudante;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected ProjetoJava() {}

    public ProjetoJava(Long idEstudante, String nome) {
        this.idEstudante = idEstudante;
        this.nome = nome;
    }

    /** Marca o projeto como alterado agora (renomear, salvar ou criar arquivo). */
    public void tocar() { this.atualizadoEm = Instant.now(); }

    public Long getId() { return id; }
    public Long getIdEstudante() { return idEstudante; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
