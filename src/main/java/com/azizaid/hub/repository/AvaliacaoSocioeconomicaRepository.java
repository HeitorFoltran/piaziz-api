package com.azizaid.hub.repository;

import com.azizaid.hub.model.AvaliacaoSocioeconomica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvaliacaoSocioeconomicaRepository extends JpaRepository<AvaliacaoSocioeconomica, Long> {

    Optional<AvaliacaoSocioeconomica> findByFichaId(Long fichaId);
}
