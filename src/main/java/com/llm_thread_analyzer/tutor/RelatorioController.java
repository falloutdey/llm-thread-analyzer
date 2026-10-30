package com.llm_thread_analyzer.tutor;

import com.llm_thread_analyzer.llm.LlmRouter;
import com.llm_thread_analyzer.tutor.entity.*;
import com.llm_thread_analyzer.tutor.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Relatórios para a análise do TCC. Protegidos por token (variável de ambiente RELATORIO_TOKEN).
 * Abra no navegador: http://localhost:8081/api/relatorios/mensagens.csv?token=SEU_TOKEN
 *
 * CSV com separador ";" e BOM UTF-8 (abre direto no Excel em português).
 *
 *   /api/relatorios/resumo          JSON: chamadas por modelo, falhas por motivo, latência
 *   /api/relatorios/submissoes.csv  uma linha por "Analisar"
 *   /api/relatorios/alertas.csv     uma linha por alerta (e se foi resolvido na tentativa seguinte)
 *   /api/relatorios/mensagens.csv   uma linha por fala (aluno, tutor, sistema) com o modelo usado
 *   /api/relatorios/chamadas.csv    uma linha por tentativa de chamada ao LLM (inclui falhas e fallback)
 *   /api/relatorios/eventos.csv     ações de tela enviadas pelo frontend
 */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final String token;
    private final SubmissaoRepository submissoes;
    private final AlertaRepository alertas;
    private final ConversaRepository conversas;
    private final MensagemRepository mensagens;
    private final ChamadaLlmRepository chamadas;
    private final EventoRepository eventos;
    private final LlmRouter llm;

    public RelatorioController(@Value("${relatorio.token:}") String token,
                               SubmissaoRepository submissoes, AlertaRepository alertas,
                               ConversaRepository conversas, MensagemRepository mensagens,
                               ChamadaLlmRepository chamadas, EventoRepository eventos, LlmRouter llm) {
        this.token = token;
        this.submissoes = submissoes;
        this.alertas = alertas;
        this.conversas = conversas;
        this.mensagens = mensagens;
        this.chamadas = chamadas;
        this.eventos = eventos;
        this.llm = llm;
    }

    @GetMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestParam(required = false) String token,
                                    @RequestHeader(value = "X-Relatorio-Token", required = false) String cab) {
        verificar(token, cab);
        List<ChamadaLlm> todas = chamadas.findAll();

        Map<String, Object> porModelo = new TreeMap<>();
        Map<String, List<ChamadaLlm>> grupos = todas.stream()
                .collect(Collectors.groupingBy(c -> c.getNomeConfig() + " (" + c.getModelo() + ")"));
        for (var e : grupos.entrySet()) {
            List<ChamadaLlm> lista = e.getValue();
            List<ChamadaLlm> ok = lista.stream().filter(ChamadaLlm::isSucesso).toList();
            Map<String, Long> falhas = lista.stream().filter(c -> !c.isSucesso())
                    .collect(Collectors.groupingBy(c -> String.valueOf(c.getMotivoFalha()), TreeMap::new, Collectors.counting()));
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("tentativas", lista.size());
            linha.put("sucessos", ok.size());
            linha.put("falhasPorMotivo", falhas);
            linha.put("latenciaMediaSucessoMs", ok.stream().mapToLong(ChamadaLlm::getLatenciaMs).average().orElse(0));
            linha.put("latenciaMaximaSucessoMs", ok.stream().mapToLong(ChamadaLlm::getLatenciaMs).max().orElse(0));
            porModelo.put(e.getKey(), linha);
        }

        List<Mensagem> todasMsgs = mensagens.findAll();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("modelosAtivos", llm.modelosAtivos());
        r.put("submissoes", submissoes.count());
        r.put("alunos", submissoes.findAll().stream().map(Submissao::getAlunoId).distinct().count());
        r.put("alertas", alertas.count());
        r.put("alertasResolvidos", alertas.findAll().stream().filter(Alerta::isResolvido).count());
        r.put("conversas", conversas.count());
        r.put("mensagensAluno", todasMsgs.stream().filter(m -> m.getAutor() == TutorEnums.Autor.ALUNO).count());
        r.put("respostasTutor", todasMsgs.stream().filter(m -> m.getAutor() == TutorEnums.Autor.TUTOR).count());
        r.put("respostasComFallback", todasMsgs.stream().filter(m -> Boolean.TRUE.equals(m.getUsouFallback()) && m.getAutor() == TutorEnums.Autor.TUTOR).count());
        r.put("falhasSemResposta", todasMsgs.stream().filter(m -> m.getTipo() == TutorEnums.TipoMensagem.FALHA_LLM).count());
        r.put("respostasCortadas", todasMsgs.stream().filter(m -> "MAX_TOKENS".equals(m.getMotivoFim())).count());
        r.put("respostasComPossivelCodigo", todasMsgs.stream().filter(m -> Boolean.TRUE.equals(m.getPossivelCodigo())).count());
        List<Mensagem> falasTutor = todasMsgs.stream().filter(m -> m.getAutor() == TutorEnums.Autor.TUTOR).toList();
        r.put("falasPorVersaoPrompt", falasTutor.stream().collect(Collectors.groupingBy(
                m -> String.valueOf(m.getVersaoPrompt()), TreeMap::new, Collectors.counting())));
        r.put("falasPorNivelDica", falasTutor.stream().filter(m -> m.getNivelDica() != null).collect(Collectors.groupingBy(
                m -> "nivel " + m.getNivelDica(), TreeMap::new, Collectors.counting())));
        r.put("avaliacoesDoAluno", falasTutor.stream().filter(m -> m.getAvaliacaoAluno() != null).collect(Collectors.groupingBy(
                Mensagem::getAvaliacaoAluno, TreeMap::new, Collectors.counting())));
        r.put("porModelo", porModelo);
        return ResponseEntity.ok(r);
    }

    /** Conversa completa com os campos internos (nível, avaliação, diagnóstico, modelo). Para inspecionar o tutor. */
    @GetMapping("/conversas/{id}")
    public ResponseEntity<?> conversa(@PathVariable Long id,
                                      @RequestParam(required = false) String token,
                                      @RequestHeader(value = "X-Relatorio-Token", required = false) String cab) {
        verificar(token, cab);
        List<Map<String, Object>> falas = new ArrayList<>();
        for (Mensagem m : mensagens.findByConversaIdOrderByOrdemAsc(id)) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("ordem", m.getOrdem());
            f.put("autor", m.getAutor());
            f.put("tipo", m.getTipo());
            f.put("versaoPrompt", m.getVersaoPrompt());
            f.put("modelo", m.getModelo());
            f.put("nivelDica", m.getNivelDica());
            f.put("avaliacaoAluno", m.getAvaliacaoAluno());
            f.put("alunoDescreveuCorrecao", m.getAlunoDescreveuCorrecao());
            f.put("possivelCodigo", m.getPossivelCodigo());
            f.put("latenciaTotalMs", m.getLatenciaTotalMs());
            f.put("diagnostico", m.getDiagnostico());
            f.put("texto", m.getTexto());
            falas.add(f);
        }
        return ResponseEntity.ok(falas);
    }

    @GetMapping("/submissoes.csv")
    public ResponseEntity<byte[]> submissoesCsv(@RequestParam(required = false) String token,
                                                @RequestHeader(value = "X-Relatorio-Token", required = false) String cab) {
        verificar(token, cab);
        Csv csv = new Csv("submissao_id", "aluno_id", "sessao_id", "arquivo", "tentativa", "modo", "compilou",
                "total_alertas", "duracao_analise_ms", "criada_em", "erro_compilacao", "codigo");
        for (Submissao s : submissoes.findAll()) {
            csv.linha(s.getId(), s.getAlunoId(), s.getSessaoId(), s.getArquivo(), s.getNumeroTentativa(), s.getModo(),
                    s.isCompilou(), s.getTotalAlertas(), s.getDuracaoAnaliseMs(), s.getCriadaEm(),
                    s.getErroCompilacao(), s.getCodigo());
        }
        return csv.resposta("submissoes.csv");
    }

    @GetMapping("/alertas.csv")
    public ResponseEntity<byte[]> alertasCsv(@RequestParam(required = false) String token,
                                             @RequestHeader(value = "X-Relatorio-Token", required = false) String cab) {
        verificar(token, cab);
        Csv csv = new Csv("alerta_id", "submissao_id", "aluno_id", "arquivo", "tentativa", "modo", "tipo", "origem",
                "linha", "resolvido", "resolvido_na_submissao_id", "resolvido_em", "mensagem");
        for (Alerta a : alertas.findAll()) {
            Submissao s = a.getSubmissao();
            csv.linha(a.getId(), s.getId(), s.getAlunoId(), s.getArquivo(), s.getNumeroTentativa(), s.getModo(),
                    a.getTipo(), a.getOrigem(), a.getLinha(), a.isResolvido(), a.getResolvidoNaSubmissaoId(),
                    a.getResolvidoEm(), a.getMensagem());
        }
        return csv.resposta("alertas.csv");
    }

    @GetMapping("/mensagens.csv")
    public ResponseEntity<byte[]> mensagensCsv(@RequestParam(required = false) String token,
                                               @RequestHeader(value = "X-Relatorio-Token", required = false) String cab,
                                               @RequestParam(defaultValue = "false") boolean incluirPrompt) {
        verificar(token, cab);
        List<String> colunas = new ArrayList<>(List.of("mensagem_id", "conversa_id", "aluno_id", "submissao_id",
                "alerta_tipo", "modo", "estado_conversa", "ordem", "autor", "tipo", "criada_em", "versao_prompt",
                "provedor", "modelo", "nome_config", "usou_fallback", "num_tentativas", "latencia_total_ms",
                "tokens_entrada", "tokens_saida", "motivo_fim", "possivel_codigo", "nivel_dica", "avaliacao_aluno",
                "aluno_descreveu_correcao", "diagnostico", "texto"));
        if (incluirPrompt) colunas.add("prompt_enviado");
        Csv csv = new Csv(colunas.toArray(String[]::new));

        for (Mensagem m : mensagens.findAll()) {
            Conversa c = m.getConversa();
            List<Object> v = new ArrayList<>(Arrays.asList(m.getId(), c.getId(), c.getAlunoId(), c.getSubmissao().getId(),
                    c.getAlerta() == null ? null : c.getAlerta().getTipo(), c.getModo(), c.getEstado(),
                    m.getOrdem(), m.getAutor(), m.getTipo(), m.getCriadaEm(), m.getVersaoPrompt(),
                    m.getProvedor(), m.getModelo(), m.getNomeConfig(), m.getUsouFallback(), m.getNumeroTentativas(),
                    m.getLatenciaTotalMs(), m.getTokensEntrada(), m.getTokensSaida(), m.getMotivoFim(), m.getPossivelCodigo(), m.getNivelDica(),
                    m.getAvaliacaoAluno(), m.getAlunoDescreveuCorrecao(), m.getDiagnostico(), m.getTexto()));
            if (incluirPrompt) v.add(m.getPromptEnviado());
            csv.linha(v.toArray());
        }
        return csv.resposta("mensagens.csv");
    }

    @GetMapping("/chamadas.csv")
    public ResponseEntity<byte[]> chamadasCsv(@RequestParam(required = false) String token,
                                              @RequestHeader(value = "X-Relatorio-Token", required = false) String cab) {
        verificar(token, cab);
        Csv csv = new Csv("chamada_id", "mensagem_id", "conversa_id", "aluno_id", "ordem", "nome_config", "provedor",
                "modelo", "sucesso", "motivo_falha", "latencia_ms", "tokens_entrada", "tokens_saida", "criada_em", "detalhe_falha");
        for (ChamadaLlm ch : chamadas.findAll()) {
            Mensagem m = ch.getMensagem();
            csv.linha(ch.getId(), m.getId(), m.getConversa().getId(), m.getConversa().getAlunoId(), ch.getOrdem(),
                    ch.getNomeConfig(), ch.getProvedor(), ch.getModelo(), ch.isSucesso(), ch.getMotivoFalha(),
                    ch.getLatenciaMs(), ch.getTokensEntrada(), ch.getTokensSaida(), ch.getCriadaEm(), ch.getDetalheFalha());
        }
        return csv.resposta("chamadas.csv");
    }

    @GetMapping("/eventos.csv")
    public ResponseEntity<byte[]> eventosCsv(@RequestParam(required = false) String token,
                                             @RequestHeader(value = "X-Relatorio-Token", required = false) String cab) {
        verificar(token, cab);
        Csv csv = new Csv("evento_id", "aluno_id", "sessao_id", "tipo", "submissao_id", "conversa_id", "criado_em", "detalhe");
        for (Evento e : eventos.findAll()) {
            csv.linha(e.getId(), e.getAlunoId(), e.getSessaoId(), e.getTipo(), e.getSubmissaoId(), e.getConversaId(),
                    e.getCriadoEm(), e.getDetalhe());
        }
        return csv.resposta("eventos.csv");
    }

    @ExceptionHandler(TutorException.class)
    public ResponseEntity<Map<String, String>> erro(TutorException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("erro", e.getMessage()));
    }

    private void verificar(String tokenParam, String tokenCabecalho) {
        if (token == null || token.isBlank()) {
            throw new TutorException(HttpStatus.FORBIDDEN,
                    "Relatórios desativados: defina a variável de ambiente RELATORIO_TOKEN e reinicie.");
        }
        String recebido = tokenCabecalho != null ? tokenCabecalho : tokenParam;
        if (recebido == null || !MessageDigest.isEqual(
                recebido.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new TutorException(HttpStatus.UNAUTHORIZED, "Token de relatório inválido.");
        }
    }

    /** Montador de CSV simples: ";" como separador, aspas em todos os campos, BOM UTF-8. */
    private static final class Csv {
        private final StringBuilder sb = new StringBuilder("﻿");

        Csv(String... cabecalho) {
            linha((Object[]) cabecalho);
        }

        void linha(Object... valores) {
            for (int i = 0; i < valores.length; i++) {
                if (i > 0) sb.append(';');
                sb.append(campo(valores[i]));
            }
            sb.append("\r\n");
        }

        private static String campo(Object v) {
            if (v == null) return "";
            String s = String.valueOf(v);
            // Evita que o Excel interprete texto do aluno como fórmula.
            if (!s.isEmpty() && "=+@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }

        ResponseEntity<byte[]> resposta(String nome) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
