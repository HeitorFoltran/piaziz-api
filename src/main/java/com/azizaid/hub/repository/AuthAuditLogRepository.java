package com.azizaid.hub.repository;

import com.azizaid.hub.model.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            SELECT COUNT(a) FROM AuthAuditLog a
            WHERE a.profissionalId = :profissionalId
              AND a.sucesso = false
              AND a.timestamp > :desde
              AND (a.motivoFalha IS NULL OR a.motivoFalha <> :motivoIgnorado)
            """)
    long contarFalhasRecentesDaConta(@Param("profissionalId") Long profissionalId,
                                     @Param("desde") LocalDateTime desde,
                                     @Param("motivoIgnorado") String motivoIgnorado);

    @Modifying
    @Query("UPDATE AuthAuditLog a SET a.ipAddress = NULL WHERE a.timestamp < :limite AND a.ipAddress IS NOT NULL")
    int limparIpAntesDe(@Param("limite") LocalDateTime limite);
}
