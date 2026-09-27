package com.azizaid.hub.repository;

import com.azizaid.hub.model.ContaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContaAuditLogRepository extends JpaRepository<ContaAuditLog, Long> {

    List<ContaAuditLog> findByProfissionalIdOrderByTimestampAsc(Long profissionalId);
}
