package com.llm_thread_analyzer.tutor.entity;

import com.llm_thread_analyzer.tutor.entity.TutorEnums.Autor;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.TipoMensagem;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Uma fala na conversa. Para falas do TUTOR, guarda também qual modelo respondeu,
 * se houve fallback, latência, tokens, o prompt enviado e a resposta bruta da API.
 */
@Entity
@Table(name = "mensagem")
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Conversa conversa;

    private int ordem;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Autor autor;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoMensagem tipo;

    @Column(length = 20_000)
    private String texto;

    private Instant criadaEm = Instant.now();

    // ---- Só para falas do tutor ----
    @Column(length = 40)
    private String versaoPrompt;
    @Column(length = 40)
    private String provedor;
    @Column(length = 80)
    private String modelo;
    @Column(length = 60)
    private String nomeConfig;
    private Boolean usouFallback;
    private Integer numeroTentativas;
    private Long latenciaTotalMs;
    private Integer tokensEntrada;
    private Integer tokensSaida;
    /** Motivo de término informado pela API (STOP, MAX_TOKENS...). MAX_TOKENS = resposta cortada. */
    @Column(length = 30)
    private String motivoFim;
    /** Verificação automática: a resposta parece conter código pronto? (guarda-corpo anti-código) */
    private Boolean possivelCodigo;
    /** Nível de dica (para o prompt socrático). Nulo no prompt v1. */
    private Integer nivelDica;
    /** Como o tutor avaliou a mensagem anterior do aluno (CORRETA, PARCIAL, INCORRETA...). Prompt v2. */
    @Column(length = 30)
    private String avaliacaoAluno;
    /** Raciocínio interno do tutor sobre o aluno (o aluno não vê). Prompt v2. */
    @Column(length = 4_000)
    private String diagnostico;
    /** O tutor julgou que o aluno já descreveu uma correção válida. Prompt v2. */
    private Boolean alunoDescreveuCorrecao;
    @Column(length = 200_000)
    private String promptEnviado;
    @Column(length = 100_000)
    private String respostaBruta;

    public Long getId() { return id; }
    public Conversa getConversa() { return conversa; }
    public void setConversa(Conversa conversa) { this.conversa = conversa; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public Autor getAutor() { return autor; }
    public void setAutor(Autor autor) { this.autor = autor; }
    public TipoMensagem getTipo() { return tipo; }
    public void setTipo(TipoMensagem tipo) { this.tipo = tipo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant criadaEm) { this.criadaEm = criadaEm; }
    public String getVersaoPrompt() { return versaoPrompt; }
    public void setVersaoPrompt(String versaoPrompt) { this.versaoPrompt = versaoPrompt; }
    public String getProvedor() { return provedor; }
    public void setProvedor(String provedor) { this.provedor = provedor; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getNomeConfig() { return nomeConfig; }
    public void setNomeConfig(String nomeConfig) { this.nomeConfig = nomeConfig; }
    public Boolean getUsouFallback() { return usouFallback; }
    public void setUsouFallback(Boolean usouFallback) { this.usouFallback = usouFallback; }
    public Integer getNumeroTentativas() { return numeroTentativas; }
    public void setNumeroTentativas(Integer numeroTentativas) { this.numeroTentativas = numeroTentativas; }
    public Long getLatenciaTotalMs() { return latenciaTotalMs; }
    public void setLatenciaTotalMs(Long latenciaTotalMs) { this.latenciaTotalMs = latenciaTotalMs; }
    public Integer getTokensEntrada() { return tokensEntrada; }
    public void setTokensEntrada(Integer tokensEntrada) { this.tokensEntrada = tokensEntrada; }
    public Integer getTokensSaida() { return tokensSaida; }
    public void setTokensSaida(Integer tokensSaida) { this.tokensSaida = tokensSaida; }
    public String getMotivoFim() { return motivoFim; }
    public void setMotivoFim(String motivoFim) { this.motivoFim = motivoFim; }
    public Boolean getPossivelCodigo() { return possivelCodigo; }
    public void setPossivelCodigo(Boolean possivelCodigo) { this.possivelCodigo = possivelCodigo; }
    public String getAvaliacaoAluno() { return avaliacaoAluno; }
    public void setAvaliacaoAluno(String avaliacaoAluno) { this.avaliacaoAluno = avaliacaoAluno; }
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }
    public Boolean getAlunoDescreveuCorrecao() { return alunoDescreveuCorrecao; }
    public void setAlunoDescreveuCorrecao(Boolean alunoDescreveuCorrecao) { this.alunoDescreveuCorrecao = alunoDescreveuCorrecao; }
    public Integer getNivelDica() { return nivelDica; }
    public void setNivelDica(Integer nivelDica) { this.nivelDica = nivelDica; }
    public String getPromptEnviado() { return promptEnviado; }
    public void setPromptEnviado(String promptEnviado) { this.promptEnviado = promptEnviado; }
    public String getRespostaBruta() { return respostaBruta; }
    public void setRespostaBruta(String respostaBruta) { this.respostaBruta = respostaBruta; }
}
