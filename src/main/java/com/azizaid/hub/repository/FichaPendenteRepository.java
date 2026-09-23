package com.azizaid.hub.repository;

import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FichaPendenteRepository extends JpaRepository<FichaPendente, Long> {

    List<FichaPendente> findByStatusOrderByDataSubmissaoDesc(StatusFichaPendente status);
}
