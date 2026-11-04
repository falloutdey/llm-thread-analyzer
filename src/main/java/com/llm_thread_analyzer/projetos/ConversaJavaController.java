package com.llm_thread_analyzer.projetos;

import com.llm_thread_analyzer.auth.AlunoAutenticado;
import com.llm_thread_analyzer.tutor.TutorException;
import com.llm_thread_analyzer.tutor.TutorService;
import com.llm_thread_analyzer.tutor.dto.TutorDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Chat do tutor no editor Java do Sumawma. São as mesmas operações de /api/tutor, mas o aluno vem
 * do token de login (ver SecurityConfig), nunca do corpo da requisição.
 *
 *   POST /api/java/conversas                     {submissaoId, alertaId}   abre (ou retoma) a conversa sobre um alerta
 *   GET  /api/java/conversas/{id}                                          lê a conversa
 *   POST /api/java/conversas/{id}/mensagens      {texto, tipo?}            aluno escreve; tipo PEDIDO_DICA = botão de dica
 *   POST /api/java/conversas/{id}/tentar-novamente                         repete quando o tutor não conseguiu responder
 *   POST /api/java/conversas/{id}/encerrar                                 aluno fecha a conversa
 *   POST /api/java/conversas/{id}/reflexao       {texto}                   explicação final do aluno (alerta resolvido)
 *   POST /api/java/eventos                       {sessaoId, tipo, detalhe?, submissaoId?, conversaId?}   ações na tela
 */
@RestController
@RequestMapping("/api/java")
public class ConversaJavaController {

    /** Corpo do POST /conversas (o aluno vem do token). */
    public record PedidoConversa(Long submissaoId, Long alertaId) {}

    /** Corpo do POST /mensagens. */
    public record PedidoMensagem(String texto, String tipo) {}

    /** Corpo do POST /eventos. */
    public record PedidoEvento(String sessaoId, String tipo, String detalhe, Long submissaoId, Long conversaId) {}

    private final TutorService tutor;

    public ConversaJavaController(TutorService tutor) {
        this.tutor = tutor;
    }

    private static String aluno(AlunoAutenticado a) {
        return String.valueOf(a.idEstudante());
    }

    @PostMapping("/conversas")
    public ConversaResponse iniciar(@AuthenticationPrincipal AlunoAutenticado a,
                                    @RequestBody(required = false) PedidoConversa pedido) {
        PedidoConversa p = pedido == null ? new PedidoConversa(null, null) : pedido;
        return tutor.iniciarConversa(new NovaConversaRequest(aluno(a), p.submissaoId(), p.alertaId()));
    }

    @GetMapping("/conversas/{id}")
    public ConversaResponse obter(@AuthenticationPrincipal AlunoAutenticado a, @PathVariable Long id) {
        return tutor.obter(id, aluno(a));
    }

    @PostMapping("/conversas/{id}/mensagens")
    public ConversaResponse enviar(@AuthenticationPrincipal AlunoAutenticado a, @PathVariable Long id,
                                   @RequestBody(required = false) PedidoMensagem pedido) {
        PedidoMensagem p = pedido == null ? new PedidoMensagem(null, null) : pedido;
        return tutor.enviarMensagem(id, new NovaMensagemRequest(aluno(a), p.texto(), p.tipo()));
    }

    @PostMapping("/conversas/{id}/tentar-novamente")
    public ConversaResponse tentarNovamente(@AuthenticationPrincipal AlunoAutenticado a, @PathVariable Long id) {
        return tutor.tentarNovamente(id, new AlunoRequest(aluno(a)));
    }

    @PostMapping("/conversas/{id}/encerrar")
    public ConversaResponse encerrar(@AuthenticationPrincipal AlunoAutenticado a, @PathVariable Long id) {
        return tutor.encerrar(id, new AlunoRequest(aluno(a)));
    }

    @PostMapping("/conversas/{id}/reflexao")
    public ConversaResponse reflexao(@AuthenticationPrincipal AlunoAutenticado a, @PathVariable Long id,
                                     @RequestBody(required = false) PedidoMensagem pedido) {
        return tutor.registrarReflexao(id, aluno(a), pedido == null ? null : pedido.texto());
    }

    @PostMapping("/eventos")
    public ResponseEntity<Void> evento(@AuthenticationPrincipal AlunoAutenticado a,
                                       @RequestBody(required = false) PedidoEvento pedido) {
        if (pedido != null) {
            tutor.registrarEvento(new EventoRequest(aluno(a), pedido.sessaoId(), pedido.tipo(), pedido.detalhe(),
                    pedido.submissaoId(), pedido.conversaId()));
        }
        return ResponseEntity.noContent().build();
    }

    // ===================== Erros =====================

    /** Conversa ou submissão de outro aluno responde 404, como nos projetos (não revela que existe). */
    @ExceptionHandler(TutorException.class)
    public ResponseEntity<Map<String, String>> erroDoTutor(TutorException e) {
        if (e.getStatus().value() == 403) {
            return ResponseEntity.status(404).body(Map.of("erro", "Conversa não encontrada."));
        }
        return ResponseEntity.status(e.getStatus()).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> requisicaoInvalida(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Requisição inválida."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> erroInterno(RuntimeException e) {
        System.err.println("[Chat Java] Erro interno: " + e);
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of("erro", "Erro interno no servidor. Tente novamente."));
    }
}
