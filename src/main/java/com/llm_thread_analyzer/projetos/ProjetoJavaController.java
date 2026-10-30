package com.llm_thread_analyzer.projetos;

import com.llm_thread_analyzer.auth.AlunoAutenticado;
import com.llm_thread_analyzer.projetos.dto.ProjetosDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/**
 * Projetos e arquivos Java do aluno logado.
 * Todas as rotas exigem "Authorization: Bearer <token do login do Sumawma>" (ver SecurityConfig).
 *
 *   GET    /api/java/projetos                              lista os projetos do aluno
 *   POST   /api/java/projetos                {nome}        cria (já com Main.java)
 *   GET    /api/java/projetos/{id}                         abre, com os arquivos e o conteúdo
 *   PUT    /api/java/projetos/{id}           {nome}        renomeia
 *   DELETE /api/java/projetos/{id}                         exclui (com os arquivos)
 *   POST   /api/java/projetos/{id}/arquivos  {nome, conteudo?}   cria arquivo
 *   PUT    /api/java/projetos/{id}/arquivos/{idArq}  {nome?, conteudo?}   salva e/ou renomeia
 *   DELETE /api/java/projetos/{id}/arquivos/{idArq}                       exclui arquivo
 */
@RestController
@RequestMapping("/api/java/projetos")
public class ProjetoJavaController {

    private final ProjetoJavaService service;

    public ProjetoJavaController(ProjetoJavaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjetoResumo> listar(@AuthenticationPrincipal AlunoAutenticado aluno) {
        return service.listar(aluno.idEstudante());
    }

    @PostMapping
    public ResponseEntity<ProjetoDetalhe> criar(@AuthenticationPrincipal AlunoAutenticado aluno,
                                                @RequestBody(required = false) NovoProjeto pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(aluno.idEstudante(), pedido));
    }

    @GetMapping("/{idProjeto}")
    public ProjetoDetalhe abrir(@AuthenticationPrincipal AlunoAutenticado aluno, @PathVariable long idProjeto) {
        return service.abrir(aluno.idEstudante(), idProjeto);
    }

    @PutMapping("/{idProjeto}")
    public ProjetoResumo renomear(@AuthenticationPrincipal AlunoAutenticado aluno, @PathVariable long idProjeto,
                                  @RequestBody(required = false) RenomearProjeto pedido) {
        return service.renomear(aluno.idEstudante(), idProjeto, pedido);
    }

    @DeleteMapping("/{idProjeto}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal AlunoAutenticado aluno, @PathVariable long idProjeto) {
        service.excluir(aluno.idEstudante(), idProjeto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idProjeto}/arquivos")
    public ResponseEntity<ArquivoDto> criarArquivo(@AuthenticationPrincipal AlunoAutenticado aluno,
                                                   @PathVariable long idProjeto,
                                                   @RequestBody(required = false) NovoArquivo pedido) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.criarArquivo(aluno.idEstudante(), idProjeto, pedido));
    }

    @PutMapping("/{idProjeto}/arquivos/{idArquivo}")
    public ArquivoDto alterarArquivo(@AuthenticationPrincipal AlunoAutenticado aluno, @PathVariable long idProjeto,
                                     @PathVariable long idArquivo,
                                     @RequestBody(required = false) AlterarArquivo pedido) {
        return service.alterarArquivo(aluno.idEstudante(), idProjeto, idArquivo, pedido);
    }

    @DeleteMapping("/{idProjeto}/arquivos/{idArquivo}")
    public ResponseEntity<Void> excluirArquivo(@AuthenticationPrincipal AlunoAutenticado aluno,
                                               @PathVariable long idProjeto, @PathVariable long idArquivo) {
        service.excluirArquivo(aluno.idEstudante(), idProjeto, idArquivo);
        return ResponseEntity.noContent().build();
    }

    // ===================== Erros =====================

    @ExceptionHandler(ProjetoException.class)
    public ResponseEntity<Map<String, String>> erroDeUso(ProjetoException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> requisicaoInvalida(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Requisição inválida."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> erroInterno(RuntimeException e) {
        System.err.println("[Projetos Java] Erro interno: " + e);
        e.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(Map.of("erro", "Erro interno no servidor. Tente novamente."));
    }
}
