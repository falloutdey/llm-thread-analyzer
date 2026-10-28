package com.llm_thread_analyzer.tutor.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Cada tentativa de chamada a um modelo, inclusive as que falharam.
 * É daqui que sai o relatório de "qual LLM respondeu, quantas vezes caiu no fallback e por quê".
 */
@Entity
@Table(name = "chamada_llm")
public class ChamadaLlm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mensagem do tutor (ou de falha) que esta tentativa tentou produzir. */
    @ManyToOne(optional = false)
    private Mensagem mensagem;

    private int ordem;

    @Column(length = 60)
    private String nomeConfig;
    @Column(length = 40)
    private String provedor;
    @Column(length = 80)
    private String modelo;

    private boolean sucesso;

    @Column(length = 30)
    private String motivoFalha;

    @Column(length = 1_000)
    private String detalheFalha;

    private long latenciaMs;
    private Integer tokensEntrada;
    private Integer tokensSaida;

    private Instant criadaEm = Instant.now();

    public Long getId() { return id; }
    public Mensagem getMensagem() { return mensagem; }
    public void setMensagem(Mensagem mensagem) { this.mensagem = mensagem; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public String getNomeConfig() { return nomeConfig; }
    public void setNomeConfig(String nomeConfig) { this.nomeConfig = nomeConfig; }
    public String getProvedor() { return provedor; }
    public void setProvedor(String provedor) { this.provedor = provedor; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }
    public String getMotivoFalha() { return motivoFalha; }
    public void setMotivoFalha(String motivoFalha) { this.motivoFalha = motivoFalha; }
    public String getDetalheFalha() { return detalheFalha; }
    public void setDetalheFalha(String detalheFalha) { this.detalheFalha = detalheFalha; }
    public long getLatenciaMs() { return latenciaMs; }
    public void setLatenciaMs(long latenciaMs) { this.latenciaMs = latenciaMs; }
    public Integer getTokensEntrada() { return tokensEntrada; }
    public void setTokensEntrada(Integer tokensEntrada) { this.tokensEntrada = tokensEntrada; }
    public Integer getTokensSaida() { return tokensSaida; }
    public void setTokensSaida(Integer tokensSaida) { this.tokensSaida = tokensSaida; }
    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant criadaEm) { this.criadaEm = criadaEm; }
}
