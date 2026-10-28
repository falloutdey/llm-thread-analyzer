package com.llm_thread_analyzer.tutor;

import com.llm_thread_analyzer.tutor.dto.TutorDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API do tutor usada pelo editor no Sumawma.
 *
 *   POST /api/tutor/submissoes                      analisa o código (sem LLM) e grava
 *   POST /api/tutor/conversas                       abre conversa sobre um alerta (1ª fala do tutor)
 *   GET  /api/tutor/conversas/{id}?alunoId=...      lê a conversa
 *   POST /api/tutor/conversas/{id}/mensagens        aluno pergunta / pede dica
 *   POST /api/tutor/conversas/{id}/tentar-novamente repete quando o LLM falhou
 *   POST /api/tutor/conversas/{id}/encerrar         aluno fecha a conversa
 *   POST /api/tutor/eventos                         registro de ações na tela
 *
 * TODO (antes de expor fora da máquina local): validar o JWT do Sumawma em vez de confiar no alunoId do corpo.
 */
@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    private final TutorService tutor;

    public TutorController(TutorService tutor) {
        this.tutor = tutor;
    }

    @PostMapping("/submissoes")
    public SubmissaoResponse submeter(@RequestBody NovaSubmissaoRequest req) {
        return tutor.submeter(req);
    }

    @PostMapping("/conversas")
    public ConversaResponse iniciarConversa(@RequestBody NovaConversaRequest req) {
        return tutor.iniciarConversa(req);
    }

    @GetMapping("/conversas/{id}")
    public ConversaResponse obter(@PathVariable Long id, @RequestParam String alunoId) {
        return tutor.obter(id, alunoId);
    }

    @PostMapping("/conversas/{id}/mensagens")
    public ConversaResponse enviarMensagem(@PathVariable Long id, @RequestBody NovaMensagemRequest req) {
        return tutor.enviarMensagem(id, req);
    }

    @PostMapping("/conversas/{id}/tentar-novamente")
    public ConversaResponse tentarNovamente(@PathVariable Long id, @RequestBody AlunoRequest req) {
        return tutor.tentarNovamente(id, req);
    }

    @PostMapping("/conversas/{id}/encerrar")
    public ConversaResponse encerrar(@PathVariable Long id, @RequestBody AlunoRequest req) {
        return tutor.encerrar(id, req);
    }

    @PostMapping("/eventos")
    public ResponseEntity<Void> evento(@RequestBody EventoRequest req) {
        tutor.registrarEvento(req);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(TutorException.class)
    public ResponseEntity<Map<String, String>> erroDeUso(TutorException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> erroInterno(RuntimeException e) {
        System.err.println("[Tutor] Erro interno: " + e);
        e.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(Map.of("erro", "Erro interno no servidor. Tente novamente."));
    }
}
