package com.azizaid.hub.repository;

import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FichaPendenteRepository extends JpaRepository<FichaPendente, Long> {

    List<FichaPendente> findByStatusOrderByDataSubmissaoDesc(StatusFichaPendente status);

    @Modifying
    @Query("DELETE FROM FichaPendente f WHERE f.status = :status AND f.dataRevisao < :limite")
    int apagarRevisadasAntesDe(@Param("status") StatusFichaPendente status, @Param("limite") LocalDateTime limite);
}
