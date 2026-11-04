package com.llm_thread_analyzer.projetos.repository;

import com.llm_thread_analyzer.projetos.entity.ProjetoJava;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjetoJavaRepository extends JpaRepository<ProjetoJava, Long> {

    List<ProjetoJava> findByIdEstudanteOrderByAtualizadoEmDesc(Long idEstudante);

    /** Busca já filtrando pelo dono: projeto de outro aluno simplesmente não aparece. */
    Optional<ProjetoJava> findByIdAndIdEstudante(Long id, Long idEstudante);

    long countByIdEstudante(Long idEstudante);
}
