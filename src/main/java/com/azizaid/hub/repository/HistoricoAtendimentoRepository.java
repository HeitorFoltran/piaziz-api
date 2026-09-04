package com.azizaid.hub.repository;

import com.azizaid.hub.model.HistoricoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoricoAtendimentoRepository extends JpaRepository<HistoricoAtendimento, Long> {

    Optional<HistoricoAtendimento> findByFichaId(Long fichaId);
}
