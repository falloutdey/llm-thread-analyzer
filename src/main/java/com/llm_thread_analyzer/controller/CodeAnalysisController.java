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

    // Rota oficial para receber o código do aluno e iniciar a análise
    @PostMapping("/analisar")
    public ResponseEntity<?> analisarCodigoThread(@RequestBody SourceCode sourceCode) {
        try {
            // Chama o serviço que orquestra a compilação, o SpotBugs e o LLM
            CodeAnalysisResults resultados = codeAnalysisService.analisarCodigo(sourceCode);
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao analisar o código: " + e.getMessage());
        }
    }
}