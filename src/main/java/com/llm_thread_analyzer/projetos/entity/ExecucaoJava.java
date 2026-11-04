package com.llm_thread_analyzer.projetos.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Registro de cada execução do programa do aluno (para a pesquisa). Um clique em Executar gera uma
 * linha; um clique em "Executar 5 vezes" gera uma linha por execução, todas com o mesmo grupo.
 * Guarda o código como estava e a saída, para ver como o aluno chegou à solução.
 * O projeto é guardado só pelo id: o registro continua mesmo se o aluno excluir o projeto.
 */
@Entity
@Table(name = "execucao_java", indexes = @Index(name = "ix_execucao_java_aluno_projeto", columnList = "idEstudante, projetoId"))
public class ExecucaoJava {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long idEstudante;

    @Column(length = 64)
    private String sessaoId;

    @Column(nullable = false)
    private Long projetoId;

    /** Número do clique no projeto (1, 2, 3...); as execuções de um mesmo clique têm o mesmo número. */
    private int numeroExecucao;

    /** Identifica o clique: as execuções de um "Executar 5 vezes" têm o mesmo grupo. */
    @Column(length = 36)
    private String grupo;

    /**
     * Ordem dentro do clique (1 a 5) e quantas execuções o aluno pediu (1 ou 5).
     * Integer (coluna sem NOT NULL) para o ddl-auto=update conseguir criar as colunas numa tabela
     * que já tem linhas; linhas antigas, sem valor, valem 1.
     */
    private Integer ordemNoGrupo = 1;
    private Integer repeticoesPedidas = 1;

    /** Arquivo aberto no editor quando clicou em Executar. */
    @Column(length = 100)
    private String arquivoAtivo;

    /** Arquivo e classe que rodaram (vazios quando não compilou ou não havia main). */
    @Column(length = 100)
    private String arquivoExecutado;

    @Column(length = 300)
    private String classeExecutada;

    private boolean compilou;

    @Column(length = 20_000)
    private String erroCompilacao;

    private Integer codigoSaida;
    private long duracaoMs;

    /** TEMPO ou SAIDA quando o sistema encerrou o programa; vazio quando terminou sozinho. */
    @Column(length = 10)
    private String interrupcao;

    private boolean saidaCortada;

    @Column(length = 20_000)
    private String saida;

    @Column(length = 210_000)
    private String codigo;

    private Instant criadaEm = Instant.now();

    public Long getId() { return id; }
    public Long getIdEstudante() { return idEstudante; }
    public void setIdEstudante(Long idEstudante) { this.idEstudante = idEstudante; }
    public String getSessaoId() { return sessaoId; }
    public void setSessaoId(String sessaoId) { this.sessaoId = sessaoId; }
    public Long getProjetoId() { return projetoId; }
    public void setProjetoId(Long projetoId) { this.projetoId = projetoId; }
    public int getNumeroExecucao() { return numeroExecucao; }
    public void setNumeroExecucao(int numeroExecucao) { this.numeroExecucao = numeroExecucao; }
    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public int getOrdemNoGrupo() { return ordemNoGrupo == null ? 1 : ordemNoGrupo; }
    public void setOrdemNoGrupo(int ordemNoGrupo) { this.ordemNoGrupo = ordemNoGrupo; }
    public int getRepeticoesPedidas() { return repeticoesPedidas == null ? 1 : repeticoesPedidas; }
    public void setRepeticoesPedidas(int repeticoesPedidas) { this.repeticoesPedidas = repeticoesPedidas; }
    public String getArquivoAtivo() { return arquivoAtivo; }
    public void setArquivoAtivo(String arquivoAtivo) { this.arquivoAtivo = arquivoAtivo; }
    public String getArquivoExecutado() { return arquivoExecutado; }
    public void setArquivoExecutado(String arquivoExecutado) { this.arquivoExecutado = arquivoExecutado; }
    public String getClasseExecutada() { return classeExecutada; }
    public void setClasseExecutada(String classeExecutada) { this.classeExecutada = classeExecutada; }
    public boolean isCompilou() { return compilou; }
    public void setCompilou(boolean compilou) { this.compilou = compilou; }
    public String getErroCompilacao() { return erroCompilacao; }
    public void setErroCompilacao(String erroCompilacao) { this.erroCompilacao = erroCompilacao; }
    public Integer getCodigoSaida() { return codigoSaida; }
    public void setCodigoSaida(Integer codigoSaida) { this.codigoSaida = codigoSaida; }
    public long getDuracaoMs() { return duracaoMs; }
    public void setDuracaoMs(long duracaoMs) { this.duracaoMs = duracaoMs; }
    public String getInterrupcao() { return interrupcao; }
    public void setInterrupcao(String interrupcao) { this.interrupcao = interrupcao; }
    public boolean isSaidaCortada() { return saidaCortada; }
    public void setSaidaCortada(boolean saidaCortada) { this.saidaCortada = saidaCortada; }
    public String getSaida() { return saida; }
    public void setSaida(String saida) { this.saida = saida; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Instant getCriadaEm() { return criadaEm; }
}
