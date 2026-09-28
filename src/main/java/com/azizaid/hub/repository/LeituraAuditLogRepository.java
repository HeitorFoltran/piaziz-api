package com.azizaid.hub.repository;

import com.azizaid.hub.model.LeituraAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LeituraAuditLogRepository extends JpaRepository<LeituraAuditLog, Long> {

    boolean existsByProfissionalIdAndFichaIdAndTimestampAfter(Long profissionalId, Long fichaId, LocalDateTime desde);

    // IdDesc desempata leituras gravadas no mesmo instante.
    List<LeituraAuditLog> findTop200ByFichaIdOrderByTimestampDescIdDesc(Long fichaId);

    @Modifying
    @Query("DELETE FROM LeituraAuditLog l WHERE l.timestamp < :limite")
    int apagarAntesDe(@Param("limite") LocalDateTime limite);
}
