package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.CodeAnalysisResults;
import com.llm_thread_analyzer.model.ConcurrencyIssue;
import com.llm_thread_analyzer.model.SourceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class CodeAnalysisService {

    @Autowired
    private ConcurrencyIssuesDetector spotBugsDetector;

    @Autowired
    private LLMFeedbackService llmFeedbackService;

    public CodeAnalysisResults analisarCodigo(SourceCode sourceCode) {
        try {
            String caminhoFicheiroClass = compilarCodigo(sourceCode);

            // PASSO 2: Analisar com o SpotBugs
            List<ConcurrencyIssue> issues = spotBugsDetector.detectConcurrencyIssues(caminhoFicheiroClass);

            for (ConcurrencyIssue issue : issues) {
                String explicacao = llmFeedbackService.gerarFeedbackDidatico(
                        issue.getMessage(),
                        sourceCode.getContent()
                );
                issue.setInterpretation(explicacao);
            }

            return new CodeAnalysisResults(issues);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro durante a análise do código: " + e.getMessage());
        }
    }

    /**
     * Escreve o código fonte num ficheiro temporário e compila-o.
     * Retorna o caminho absoluto para o ficheiro .class gerado.
     */
    private String compilarCodigo(SourceCode sourceCode) throws IOException {
        // 1. Criar um diretório temporário para isolar a compilação
        Path tempDir = Files.createTempDirectory("thread-analyzer-");
        
        String nomeFicheiro = sourceCode.getFileName();
        if (nomeFicheiro == null || nomeFicheiro.isEmpty()) {
            nomeFicheiro = "Main.java";
        } else if (!nomeFicheiro.endsWith(".java")) {
            nomeFicheiro += ".java";
        }

        File sourceFile = new File(tempDir.toFile(), nomeFicheiro);
        try (FileWriter writer = new FileWriter(sourceFile)) {
            writer.write(sourceCode.getContent());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Compilador Java não encontrado. Certifique-se de que o projeto está a correr num JDK e não num JRE.");
        }

        int result = compiler.run(null, null, null, sourceFile.getAbsolutePath());
        if (result != 0) {
            throw new RuntimeException("Erro de compilação: O código fornecido contém erros de sintaxe e não pôde ser compilado.");
        }

        String nomeFicheiroClass = nomeFicheiro.replace(".java", ".class");
        File classFile = new File(tempDir.toFile(), nomeFicheiroClass);
        
        if (!classFile.exists()) {
            throw new RuntimeException("O ficheiro .class não foi gerado após a compilação.");
        }

        return classFile.getAbsolutePath();
    }
}