package com.llm_thread_analyzer.projetos.repository;

import com.llm_thread_analyzer.projetos.entity.ArquivoJava;
import com.llm_thread_analyzer.projetos.entity.ProjetoJava;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArquivoJavaRepository extends JpaRepository<ArquivoJava, Long> {

    List<ArquivoJava> findByProjetoOrderByNomeAsc(ProjetoJava projeto);

    Optional<ArquivoJava> findByIdAndProjeto(Long id, ProjetoJava projeto);

    boolean existsByProjetoAndNomeIgnoreCase(ProjetoJava projeto, String nome);

    long countByProjeto(ProjetoJava projeto);

    /** Quantidade de arquivos de cada projeto, numa consulta só: linhas [idProjeto, quantidade]. */
    @Query("select a.projeto.id, count(a) from ArquivoJava a where a.projeto.id in :ids group by a.projeto.id")
    List<Object[]> contarPorProjeto(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from ArquivoJava a where a.projeto = :projeto")
    void apagarDoProjeto(@Param("projeto") ProjetoJava projeto);
}
