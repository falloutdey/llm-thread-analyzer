package com.llm_thread_analyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.llm_thread_analyzer.model.SourceCode;
import com.llm_thread_analyzer.model.CodeAnalysisResults;
import com.llm_thread_analyzer.service.CodeAnalysisService;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class CodeAnalysisController {
    
    @Autowired
    private CodeAnalysisService codeAnalysisService;

    @PostMapping("/analisar")
    public ResponseEntity<?> analisarCodigoThread(@RequestBody SourceCode sourceCode) {
        try {
            CodeAnalysisResults resultados = codeAnalysisService.analisarCodigo(sourceCode);
            return ResponseEntity.ok(resultados);
            
        } catch (RuntimeException e) {
            // Verifica se é um erro de sintaxe do aluno
            if (e.getMessage() != null && e.getMessage().startsWith("ERRO_COMPILACAO:")) {
                String detalhesErro = e.getMessage().replace("ERRO_COMPILACAO:", "").trim();
                
                // Retorna 400 Bad Request (O aluno errou, não o servidor)
                return ResponseEntity.badRequest().body("Erro de Sintaxe! O teu código não compila. Por favor, corrige os seguintes erros antes de analisar as threads:\n\n" + detalhesErro);
            }
            
            // Outros erros de execução
            return ResponseEntity.internalServerError().body("Erro interno: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro inesperado ao analisar o código: " + e.getMessage());
        }
    }
}