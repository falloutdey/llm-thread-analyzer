package com.llm_thread_analyzer.projetos.repository;

import com.llm_thread_analyzer.projetos.entity.ExecucaoJava;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExecucaoJavaRepository extends JpaRepository<ExecucaoJava, Long> {

    /**
     * Quantos cliques em Executar o aluno já fez no projeto: conta só a 1ª execução de cada clique
     * (linhas antigas, de antes do "Executar 5 vezes", não têm ordem e também contam).
     */
    @Query("select count(e) from ExecucaoJava e where e.idEstudante = :aluno and e.projetoId = :projeto "
            + "and (e.ordemNoGrupo = 1 or e.ordemNoGrupo is null)")
    long contarCliques(@Param("aluno") Long idEstudante, @Param("projeto") Long projetoId);
}
