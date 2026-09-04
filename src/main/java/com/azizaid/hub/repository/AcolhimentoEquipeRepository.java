package com.azizaid.hub.repository;

import com.azizaid.hub.model.AcolhimentoEquipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcolhimentoEquipeRepository extends JpaRepository<AcolhimentoEquipe, Long> {

    Optional<AcolhimentoEquipe> findByFichaId(Long fichaId);
}
