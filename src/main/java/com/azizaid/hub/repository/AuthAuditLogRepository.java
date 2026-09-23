package com.azizaid.hub.repository;

import com.azizaid.hub.model.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

    long countByEmailTentadoAndSucessoFalseAndTimestampAfter(String emailTentado, LocalDateTime desde);
}
