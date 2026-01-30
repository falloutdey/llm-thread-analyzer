package com.llm_thread_analyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.llm_thread_analyzer.model.SourceCode;
import com.llm_thread_analyzer.service.CodeAnalysisService;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class CodeAnalysisController {
    
    @Autowired
    private CodeAnalysisService codeAnalysisService;

    @PutMapping("/arquivos/conteudo")
    public ResponseEntity<String> receberCodigoTeste(@RequestBody SourceCode sourceCode) {
        codeAnalysisService.testarConexao();
        return ResponseEntity.ok("Código Recebido com sucesso");
    }

}
