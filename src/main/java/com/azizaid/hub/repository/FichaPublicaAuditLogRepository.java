package com.azizaid.hub.repository;

import com.azizaid.hub.model.FichaPublicaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface FichaPublicaAuditLogRepository extends JpaRepository<FichaPublicaAuditLog, Long> {

    @Modifying
    @Query("UPDATE FichaPublicaAuditLog a SET a.ip = NULL WHERE a.timestamp < :limite AND a.ip IS NOT NULL")
    int limparIpAntesDe(@Param("limite") LocalDateTime limite);
}
