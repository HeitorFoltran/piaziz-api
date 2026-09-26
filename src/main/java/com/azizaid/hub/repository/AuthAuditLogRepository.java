package com.azizaid.hub.repository;

import com.azizaid.hub.model.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    @Query("""
            SELECT COUNT(a) FROM AuthAuditLog a
            WHERE a.emailTentado = :email
              AND a.sucesso = false
              AND a.timestamp > :desde
              AND (a.motivoFalha IS NULL OR a.motivoFalha <> :motivoIgnorado)
            """)
    long contarFalhasRecentes(@Param("email") String email,
                             @Param("desde") LocalDateTime desde,
                             @Param("motivoIgnorado") String motivoIgnorado);
}
