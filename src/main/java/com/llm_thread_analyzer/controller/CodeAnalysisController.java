package com.llm_thread_analyzer.controller;

import com.llm_thread_analyzer.model.CodeAnalysisResults;
import com.llm_thread_analyzer.model.SourceCode;
import com.llm_thread_analyzer.service.CodeAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
// @CrossOrigin removido — CORS já está configurado globalmente no SecurityConfig.java
public class CodeAnalysisController {

    @Autowired
    private CodeAnalysisService codeAnalysisService;

    /**
     * Analisa um único ficheiro de código fonte.
     * POST /api/files/analisar
     * Body: { "fileName": "Main.java", "content": "..." }
     */
    @PostMapping("/analisar")
    public ResponseEntity<?> analisarCodigoThread(@RequestBody SourceCode sourceCode) {
        try {
            CodeAnalysisResults resultados = codeAnalysisService.analisarCodigo(sourceCode);
            return ResponseEntity.ok(resultados);

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("ERRO_COMPILACAO:")) {
                String detalhesErro = e.getMessage().replace("ERRO_COMPILACAO:", "").trim();
                return ResponseEntity.badRequest().body(
                    "Erro de Sintaxe! O teu código não compila. Por favor, corrige os seguintes erros antes de analisar as threads:\n\n" + detalhesErro
                );
            }
            if (e.getMessage() != null && e.getMessage().startsWith("ERRO_ESTRUTURA:")) {
                String detalhesErro = e.getMessage().replace("ERRO_ESTRUTURA:", "").trim();
                return ResponseEntity.badRequest().body(detalhesErro);
            }
            System.err.println("[Controller] Erro interno em /analisar: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                "Ocorreu um erro interno no servidor ao processar o código. Tenta novamente mais tarde."
            );
        } catch (Exception e) {
            System.err.println("[Controller] Erro inesperado em /analisar: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                "Ocorreu um erro interno no servidor ao processar o código. Tenta novamente mais tarde."
            );
        }
    }

    /**
     * Analisa uma lista de ficheiros em sequência — ideal para rodar a bateria completa do benchmark JCB.
     * POST /api/files/analisar/batch
     *
     * Body: [
     *   { "fileName": "BugFile1.java", "content": "..." },
     *   { "fileName": "BugFile2.java", "content": "..." }
     * ]
     *
     * Resposta: lista de CodeAnalysisResults na mesma ordem do input.
     * Ficheiros com erro de compilação ou falha interna retornam com batchError: true
     * e não interrompem o processamento dos restantes.
     *
     * Para baterias grandes, considere o rate limit do Gemini (~15 RPM no tier gratuito).
     * O delay de 4500ms já está aplicado internamente entre cada chamada ao LLM.
     */
    @PostMapping("/analisar/batch")
    public ResponseEntity<?> analisarBatch(@RequestBody List<SourceCode> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return ResponseEntity.badRequest().body("A lista de ficheiros enviada está vazia.");
        }

        try {
            List<CodeAnalysisResults> resultados = codeAnalysisService.analisarBatch(codigos);
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            System.err.println("[Controller] Erro inesperado em /analisar/batch: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                "Ocorreu um erro interno no servidor ao processar o batch. Tenta novamente mais tarde."
            );
        }
    }
}