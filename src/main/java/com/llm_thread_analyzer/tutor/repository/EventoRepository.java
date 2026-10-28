package com.llm_thread_analyzer.tutor.repository;

import com.llm_thread_analyzer.tutor.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, Long> {
}
