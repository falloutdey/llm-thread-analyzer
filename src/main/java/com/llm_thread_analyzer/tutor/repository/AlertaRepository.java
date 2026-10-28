package com.llm_thread_analyzer.tutor.repository;

import com.llm_thread_analyzer.tutor.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findBySubmissaoIdOrderByIdAsc(Long submissaoId);
}
