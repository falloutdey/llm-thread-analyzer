package com.llm_thread_analyzer.tutor.repository;

import com.llm_thread_analyzer.tutor.entity.Submissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissaoRepository extends JpaRepository<Submissao, Long> {

    int countByAlunoIdAndArquivo(String alunoId, String arquivo);

    int countByAlunoIdAndProjetoId(String alunoId, Long projetoId);

    /** Última submissão que compilou, do mesmo aluno e projeto, anterior à informada. */
    Optional<Submissao> findFirstByAlunoIdAndProjetoIdAndCompilouTrueAndIdLessThanOrderByIdDesc(
            String alunoId, Long projetoId, Long id);

    /** Última submissão que compilou, do mesmo aluno e arquivo, anterior à informada. */
    Optional<Submissao> findFirstByAlunoIdAndArquivoAndCompilouTrueAndIdLessThanOrderByIdDesc(
            String alunoId, String arquivo, Long id);
}
