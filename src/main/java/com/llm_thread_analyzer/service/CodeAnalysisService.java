package com.llm_thread_analyzer.service;

import com.llm_thread_analyzer.model.CodeAnalysisResults;
import com.llm_thread_analyzer.model.ConcurrencyIssue;
import com.llm_thread_analyzer.model.SourceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
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

            List<ConcurrencyIssue> issues = spotBugsDetector.detectConcurrencyIssues(caminhoFicheiroClass);

            for (ConcurrencyIssue issue : issues) {
                String explicacao = llmFeedbackService.gerarFeedbackDidatico(
                        issue.getMessage(),
                        sourceCode.getContent()
                );
                issue.setInterpretation(explicacao);
            }

            return new CodeAnalysisResults(issues);

        } catch (RuntimeException e) {
            // Se for um erro que nós já tratámos (como o de compilação), passamos para cima
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro interno durante a análise do código: " + e.getMessage());
        }
    }

    private String compilarCodigo(SourceCode sourceCode) throws IOException {
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
            throw new RuntimeException("Compilador Java não encontrado.");
        }

        // Criamos um fluxo para capturar os erros de sintaxe do Java
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        // Adicionada a flag "-g" e o redirecionamento de erros para o errorStream
        int result = compiler.run(null, null, errorStream, "-g", sourceFile.getAbsolutePath());
        
        if (result != 0) {
            // Transformamos os erros capturados numa String
            String errosCompilacao = new String(errorStream.toByteArray());
            
            // Lançamos uma exceção com um "código" no início para o Controller identificar
            throw new RuntimeException("ERRO_COMPILACAO:" + errosCompilacao);
        }

        String nomeFicheiroClass = nomeFicheiro.replace(".java", ".class");
        File classFile = new File(tempDir.toFile(), nomeFicheiroClass);
        
        if (!classFile.exists()) {
            throw new RuntimeException("O ficheiro .class não foi gerado após a compilação.");
        }

        return classFile.getAbsolutePath();
    }
}