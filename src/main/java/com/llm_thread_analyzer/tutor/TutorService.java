package com.llm_thread_analyzer.tutor;

import com.llm_thread_analyzer.llm.LlmRouter;
import com.llm_thread_analyzer.llm.LlmTipos.Requisicao;
import com.llm_thread_analyzer.llm.LlmTipos.Resultado;
import com.llm_thread_analyzer.llm.LlmTipos.Tentativa;
import com.llm_thread_analyzer.model.ConcurrencyIssue;
import com.llm_thread_analyzer.model.SourceCode;
import com.llm_thread_analyzer.service.CodeAnalysisService;
import com.llm_thread_analyzer.tutor.dto.TutorDtos.*;
import com.llm_thread_analyzer.tutor.entity.*;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.Autor;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.EstadoConversa;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.Modo;
import com.llm_thread_analyzer.tutor.entity.TutorEnums.TipoMensagem;
import com.llm_thread_analyzer.tutor.prompt.EstrategiaPrompt;
import com.llm_thread_analyzer.tutor.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fluxo do tutor, na ordem em que acontece para o aluno:
 *   1. submeter(): aluno clica em "Analisar". Compila, roda a análise estática (sem LLM),
 *      grava a submissão e os alertas e verifica quais alertas da tentativa anterior sumiram.
 *   2. iniciarConversa(): aluno abre um alerta. O tutor gera a primeira fala (aqui entra o LLM).
 *   3. enviarMensagem(): aluno pergunta ou pede dica; o tutor responde com o histórico da conversa.
 *   4. encerrar(): aluno fecha a conversa. Ou ela é encerrada sozinha quando o alerta é resolvido
 *      numa nova submissão, ou quando atinge o limite de mensagens.
 *
 * Nenhum método segura transação de banco durante a chamada ao LLM (cada save é curto).
 */
@Service
public class TutorService {

    private static final int MAX_ALUNO_ID = 64;

    private final CodeAnalysisService analise;
    private final LlmRouter llm;
    private final SubmissaoRepository submissoes;
    private final AlertaRepository alertas;
    private final ConversaRepository conversas;
    private final MensagemRepository mensagens;
    private final ChamadaLlmRepository chamadas;
    private final EventoRepository eventos;
    private final Map<String, EstrategiaPrompt> estrategias;

    private final String versaoPromptAtual;
    private final Modo modoPadrao;
    private final int maxMensagensAluno;
    private final int maxCaracteresMensagem;
    private final boolean permitirFallback;

    public TutorService(CodeAnalysisService analise,
                        LlmRouter llm,
                        SubmissaoRepository submissoes,
                        AlertaRepository alertas,
                        ConversaRepository conversas,
                        MensagemRepository mensagens,
                        ChamadaLlmRepository chamadas,
                        EventoRepository eventos,
                        List<EstrategiaPrompt> estrategias,
                        @Value("${tutor.versao-prompt:v1-explicativo}") String versaoPromptAtual,
                        @Value("${tutor.modo-padrao:HIBRIDO}") String modoPadrao,
                        @Value("${tutor.max-mensagens-aluno:20}") int maxMensagensAluno,
                        @Value("${tutor.max-caracteres-mensagem:2000}") int maxCaracteresMensagem,
                        @Value("${tutor.permitir-fallback:true}") boolean permitirFallback) {
        this.analise = analise;
        this.llm = llm;
        this.submissoes = submissoes;
        this.alertas = alertas;
        this.conversas = conversas;
        this.mensagens = mensagens;
        this.chamadas = chamadas;
        this.eventos = eventos;
        this.estrategias = estrategias.stream()
                .collect(Collectors.toMap(EstrategiaPrompt::versao, Function.identity()));
        if (!this.estrategias.containsKey(versaoPromptAtual)) {
            throw new IllegalStateException("tutor.versao-prompt='" + versaoPromptAtual
                    + "' não existe. Disponíveis: " + this.estrategias.keySet());
        }
        this.versaoPromptAtual = versaoPromptAtual;
        this.modoPadrao = Modo.valueOf(modoPadrao.trim().toUpperCase());
        this.maxMensagensAluno = maxMensagensAluno;
        this.maxCaracteresMensagem = maxCaracteresMensagem;
        this.permitirFallback = permitirFallback;
        System.out.println("[Tutor] prompt=" + versaoPromptAtual + " modo-padrao=" + this.modoPadrao
                + " modelos=" + llm.modelosAtivos());
    }

    // =====================================================================
    // 1. Submissão
    // =====================================================================

    public SubmissaoResponse submeter(NovaSubmissaoRequest req) {
        String alunoId = exigirAluno(req.alunoId());
        if (req.content() == null || req.content().isBlank()) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "O código enviado está vazio.");
        }
        if (req.content().length() > 50_000) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "Código longo demais (máximo 50.000 caracteres).");
        }

        String arquivo = (req.fileName() == null || req.fileName().isBlank()) ? "Main.java" : req.fileName().trim();
        Modo modo = lerModo(req.modo());

        Submissao s = new Submissao();
        s.setAlunoId(alunoId);
        s.setSessaoId(cortar(req.sessaoId(), 64));
        s.setArquivo(cortar(arquivo, 255));
        s.setNumeroTentativa(submissoes.countByAlunoIdAndArquivo(alunoId, s.getArquivo()) + 1);
        s.setCodigo(req.content());
        s.setModo(modo);

        long t0 = System.nanoTime();
        List<ConcurrencyIssue> issues = List.of();
        try {
            issues = analise.detectarProblemas(new SourceCode(arquivo, req.content()));
            s.setCompilou(true);
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.startsWith("ERRO_COMPILACAO:") || msg.startsWith("ERRO_ESTRUTURA:")) {
                s.setCompilou(false);
                s.setErroCompilacao(cortar(msg.substring(msg.indexOf(':') + 1).trim(), 20_000));
            } else {
                throw e;
            }
        }
        s.setDuracaoAnaliseMs((System.nanoTime() - t0) / 1_000_000L);
        s.setTotalAlertas(issues.size());
        s = submissoes.save(s);

        List<Alerta> novos = new ArrayList<>();
        for (ConcurrencyIssue issue : issues) {
            Alerta a = new Alerta();
            a.setSubmissao(s);
            a.setTipo(cortar(issue.getType(), 120));
            a.setOrigem(issue.getType() != null && issue.getType().startsWith("SRC_") ? "FONTE" : "SPOTBUGS");
            a.setLinha(issue.getLineNumber());
            a.setMensagem(cortar(issue.getMessage(), 4_000));
            novos.add(alertas.save(a));
        }

        List<String> resolvidos = s.isCompilou() ? verificarResolucao(s, novos) : List.of();

        // No modo LLM_ISOLADO o aluno não vê os alertas (eles ficam gravados para a análise).
        List<AlertaDto> visiveis = modo == Modo.HIBRIDO
                ? novos.stream().map(TutorService::paraDto).toList()
                : List.of();

        return new SubmissaoResponse(s.getId(), s.getNumeroTentativa(), modo.name(), s.isCompilou(),
                s.getErroCompilacao(), visiveis, modo == Modo.HIBRIDO ? resolvidos : List.of());
    }

    /**
     * Compara com a submissão anterior (que compilou) do mesmo aluno e arquivo.
     * Um alerta anterior é considerado resolvido se o mesmo TIPO não aparece mais na nova submissão.
     * (Comparar por tipo, e não por linha, porque as linhas mudam quando o aluno edita.)
     */
    private List<String> verificarResolucao(Submissao nova, List<Alerta> alertasNovos) {
        Optional<Submissao> anterior = submissoes
                .findFirstByAlunoIdAndArquivoAndCompilouTrueAndIdLessThanOrderByIdDesc(
                        nova.getAlunoId(), nova.getArquivo(), nova.getId());
        if (anterior.isEmpty()) return List.of();

        Set<String> tiposAtuais = alertasNovos.stream().map(Alerta::getTipo).collect(Collectors.toSet());
        Set<String> resolvidos = new LinkedHashSet<>();

        for (Alerta a : alertas.findBySubmissaoIdOrderByIdAsc(anterior.get().getId())) {
            if (a.isResolvido() || tiposAtuais.contains(a.getTipo())) continue;
            a.setResolvido(true);
            a.setResolvidoNaSubmissaoId(nova.getId());
            a.setResolvidoEm(Instant.now());
            alertas.save(a);
            resolvidos.add(a.getTipo());

            for (Conversa c : conversas.findByAlertaIdAndEstado(a.getId(), EstadoConversa.ABERTA)) {
                c.setEstado(EstadoConversa.RESOLVIDA);
                c.setEncerradaEm(Instant.now());
                conversas.save(c);
                salvarAviso(c, "A nova análise não aponta mais este problema. Bom trabalho!");
            }
        }
        return new ArrayList<>(resolvidos);
    }

    // =====================================================================
    // 2. Conversa
    // =====================================================================

    public ConversaResponse iniciarConversa(NovaConversaRequest req) {
        String alunoId = exigirAluno(req.alunoId());
        if (req.submissaoId() == null) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "submissaoId é obrigatório.");
        }
        Submissao s = submissoes.findById(req.submissaoId())
                .orElseThrow(() -> new TutorException(HttpStatus.NOT_FOUND, "Submissão não encontrada."));
        if (!s.getAlunoId().equals(alunoId)) {
            throw new TutorException(HttpStatus.FORBIDDEN, "Esta submissão é de outro aluno.");
        }
        if (!s.isCompilou()) {
            throw new TutorException(HttpStatus.CONFLICT, "O código não compilou; corrija a compilação antes de conversar com o tutor.");
        }

        Alerta alerta = null;
        if (s.getModo() == Modo.HIBRIDO) {
            if (req.alertaId() == null) {
                throw new TutorException(HttpStatus.BAD_REQUEST, "alertaId é obrigatório no modo HIBRIDO.");
            }
            alerta = alertas.findById(req.alertaId())
                    .filter(a -> a.getSubmissao().getId().equals(s.getId()))
                    .orElseThrow(() -> new TutorException(HttpStatus.NOT_FOUND, "Alerta não encontrado nesta submissão."));

            // Reabrir o mesmo alerta continua a conversa que já existe.
            List<Conversa> abertas = conversas.findByAlertaIdAndEstado(alerta.getId(), EstadoConversa.ABERTA);
            if (!abertas.isEmpty()) {
                return resposta(abertas.get(0));
            }
        }

        Conversa c = new Conversa();
        c.setSubmissao(s);
        c.setAlerta(alerta);
        c.setAlunoId(alunoId);
        c.setModo(s.getModo());
        c.setVersaoPrompt(versaoPromptAtual);
        c = conversas.save(c);

        gerarFalaTutor(c, TipoMensagem.ABERTURA);
        return resposta(c);
    }

    public ConversaResponse enviarMensagem(Long conversaId, NovaMensagemRequest req) {
        Conversa c = carregarConversaDoAluno(conversaId, req.alunoId());
        if (c.getEstado() != EstadoConversa.ABERTA) {
            throw new TutorException(HttpStatus.CONFLICT, "Esta conversa já foi encerrada (" + c.getEstado() + ").");
        }

        TipoMensagem tipo = "PEDIDO_DICA".equalsIgnoreCase(req.tipo()) ? TipoMensagem.PEDIDO_DICA : TipoMensagem.PERGUNTA;
        String texto = req.texto() == null ? "" : req.texto().trim();
        if (texto.isEmpty() && tipo == TipoMensagem.PEDIDO_DICA) {
            texto = "Pode me dar uma dica?";
        }
        if (texto.isEmpty()) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "A mensagem está vazia.");
        }
        if (texto.length() > maxCaracteresMensagem) {
            throw new TutorException(HttpStatus.BAD_REQUEST,
                    "Mensagem longa demais (máximo " + maxCaracteresMensagem + " caracteres).");
        }

        if (mensagens.countByConversaIdAndAutor(c.getId(), Autor.ALUNO) >= maxMensagensAluno) {
            c.setEstado(EstadoConversa.LIMITE_ATINGIDO);
            c.setEncerradaEm(Instant.now());
            conversas.save(c);
            salvarAviso(c, "Esta conversa atingiu o limite de mensagens. Reanalise o código para continuar.");
            return resposta(c);
        }

        Mensagem m = new Mensagem();
        m.setConversa(c);
        m.setOrdem(proximaOrdem(c));
        m.setAutor(Autor.ALUNO);
        m.setTipo(tipo);
        m.setTexto(texto);
        mensagens.save(m);

        gerarFalaTutor(c, TipoMensagem.RESPOSTA);
        return resposta(c);
    }

    /** Repete a geração quando a última fala foi uma falha do LLM. */
    public ConversaResponse tentarNovamente(Long conversaId, AlunoRequest req) {
        Conversa c = carregarConversaDoAluno(conversaId, req.alunoId());
        List<Mensagem> hist = mensagens.findByConversaIdOrderByOrdemAsc(c.getId());
        boolean ultimaFalhou = !hist.isEmpty() && hist.get(hist.size() - 1).getTipo() == TipoMensagem.FALHA_LLM;
        if (c.getEstado() == EstadoConversa.ABERTA && ultimaFalhou) {
            boolean temFalaTutor = hist.stream().anyMatch(x -> x.getAutor() == Autor.TUTOR);
            gerarFalaTutor(c, temFalaTutor ? TipoMensagem.RESPOSTA : TipoMensagem.ABERTURA);
        }
        return resposta(c);
    }

    public ConversaResponse encerrar(Long conversaId, AlunoRequest req) {
        Conversa c = carregarConversaDoAluno(conversaId, req.alunoId());
        if (c.getEstado() == EstadoConversa.ABERTA) {
            c.setEstado(EstadoConversa.ENCERRADA_PELO_ALUNO);
            c.setEncerradaEm(Instant.now());
            conversas.save(c);
        }
        return resposta(c);
    }

    public ConversaResponse obter(Long conversaId, String alunoId) {
        return resposta(carregarConversaDoAluno(conversaId, alunoId));
    }

    // =====================================================================
    // 3. Eventos de tela
    // =====================================================================

    public void registrarEvento(EventoRequest req) {
        Evento e = new Evento();
        e.setAlunoId(cortar(req.alunoId(), MAX_ALUNO_ID));
        e.setSessaoId(cortar(req.sessaoId(), 64));
        e.setTipo(cortar(req.tipo() == null ? "DESCONHECIDO" : req.tipo(), 60));
        e.setDetalhe(cortar(req.detalhe(), 4_000));
        e.setSubmissaoId(req.submissaoId());
        e.setConversaId(req.conversaId());
        eventos.save(e);
    }

    // =====================================================================
    // Geração da fala do tutor (chamada ao LLM + registro completo)
    // =====================================================================

    private Mensagem gerarFalaTutor(Conversa c, TipoMensagem tipo) {
        List<Mensagem> historico = mensagens.findByConversaIdOrderByOrdemAsc(c.getId());
        EstrategiaPrompt estrategia = estrategias.getOrDefault(c.getVersaoPrompt(), estrategias.get(versaoPromptAtual));
        Requisicao requisicao = estrategia.montar(c, historico);

        Resultado r = llm.gerar(requisicao, permitirFallback);

        Mensagem m = new Mensagem();
        m.setConversa(c);
        m.setOrdem(historico.isEmpty() ? 1 : historico.get(historico.size() - 1).getOrdem() + 1);
        m.setVersaoPrompt(estrategia.versao());
        m.setPromptEnviado(cortar(EstrategiaPrompt.comoTexto(requisicao), 200_000));
        m.setLatenciaTotalMs(r.latenciaTotalMs());
        m.setNumeroTentativas(r.tentativas().size());
        m.setUsouFallback(r.usouFallback());

        if (r.sucesso()) {
            Tentativa ok = r.tentativas().get(r.tentativas().size() - 1);
            EstrategiaPrompt.Interpretacao fala = estrategia.interpretar(r.resposta().texto(), c, historico);
            m.setAutor(Autor.TUTOR);
            m.setTipo(tipo);
            m.setTexto(cortar(fala.texto(), 20_000));
            m.setNivelDica(fala.nivelDica());
            m.setAvaliacaoAluno(cortar(fala.avaliacaoAluno(), 30));
            m.setDiagnostico(cortar(fala.diagnostico(), 4_000));
            m.setAlunoDescreveuCorrecao(fala.alunoDescreveuCorrecao());
            m.setProvedor(r.resposta().provedor());
            m.setModelo(r.resposta().modelo());
            m.setNomeConfig(ok.nomeConfig());
            m.setTokensEntrada(r.resposta().tokensEntrada());
            m.setTokensSaida(r.resposta().tokensSaida());
            m.setMotivoFim(cortar(r.resposta().motivoFim(), 30));
            m.setRespostaBruta(cortar(r.resposta().respostaBruta(), 100_000));
            m.setPossivelCodigo(GuardaCodigo.pareceCodigo(fala.texto()));
        } else {
            m.setAutor(Autor.SISTEMA);
            m.setTipo(TipoMensagem.FALHA_LLM);
            m.setTexto("O tutor não conseguiu responder agora. Tente novamente em alguns instantes.");
        }
        m = mensagens.save(m);

        for (Tentativa t : r.tentativas()) {
            ChamadaLlm ch = new ChamadaLlm();
            ch.setMensagem(m);
            ch.setOrdem(t.ordem());
            ch.setNomeConfig(t.nomeConfig());
            ch.setProvedor(t.provedor());
            ch.setModelo(t.modelo());
            ch.setSucesso(t.sucesso());
            ch.setMotivoFalha(t.motivoFalha() == null ? null : t.motivoFalha().name());
            ch.setDetalheFalha(cortar(t.detalheFalha(), 1_000));
            ch.setLatenciaMs(t.latenciaMs());
            ch.setTokensEntrada(t.tokensEntrada());
            ch.setTokensSaida(t.tokensSaida());
            chamadas.save(ch);
        }
        return m;
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private Conversa carregarConversaDoAluno(Long conversaId, String alunoId) {
        String aluno = exigirAluno(alunoId);
        Conversa c = conversas.findById(conversaId)
                .orElseThrow(() -> new TutorException(HttpStatus.NOT_FOUND, "Conversa não encontrada."));
        if (!c.getAlunoId().equals(aluno)) {
            throw new TutorException(HttpStatus.FORBIDDEN, "Esta conversa é de outro aluno.");
        }
        return c;
    }

    private void salvarAviso(Conversa c, String texto) {
        Mensagem m = new Mensagem();
        m.setConversa(c);
        m.setOrdem(proximaOrdem(c));
        m.setAutor(Autor.SISTEMA);
        m.setTipo(TipoMensagem.AVISO);
        m.setTexto(texto);
        mensagens.save(m);
    }

    private int proximaOrdem(Conversa c) {
        List<Mensagem> hist = mensagens.findByConversaIdOrderByOrdemAsc(c.getId());
        return hist.isEmpty() ? 1 : hist.get(hist.size() - 1).getOrdem() + 1;
    }

    private ConversaResponse resposta(Conversa c) {
        List<Mensagem> hist = mensagens.findByConversaIdOrderByOrdemAsc(c.getId());
        List<MensagemDto> dtos = hist.stream()
                .map(m -> new MensagemDto(m.getId(), m.getOrdem(), m.getAutor().name(),
                        m.getTipo() == null ? null : m.getTipo().name(), m.getTexto(), m.getCriadaEm(), m.getModelo()))
                .toList();
        boolean falha = !hist.isEmpty() && hist.get(hist.size() - 1).getTipo() == TipoMensagem.FALHA_LLM;
        long doAluno = hist.stream().filter(m -> m.getAutor() == Autor.ALUNO).count();
        int restantes = (int) Math.max(0, maxMensagensAluno - doAluno);
        return new ConversaResponse(c.getId(), c.getSubmissao().getId(),
                c.getAlerta() == null ? null : c.getAlerta().getId(),
                c.getModo().name(), c.getEstado().name(), c.getVersaoPrompt(), dtos, falha, restantes);
    }

    private Modo lerModo(String modo) {
        if (modo == null || modo.isBlank()) return modoPadrao;
        try {
            return Modo.valueOf(modo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "modo inválido: use HIBRIDO ou LLM_ISOLADO.");
        }
    }

    private static String exigirAluno(String alunoId) {
        if (alunoId == null || alunoId.isBlank()) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "alunoId é obrigatório.");
        }
        if (alunoId.length() > MAX_ALUNO_ID) {
            throw new TutorException(HttpStatus.BAD_REQUEST, "alunoId longo demais.");
        }
        return alunoId.trim();
    }

    private static AlertaDto paraDto(Alerta a) {
        return new AlertaDto(a.getId(), a.getTipo(), a.getOrigem(), a.getLinha(), a.getMensagem());
    }

    static String cortar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
