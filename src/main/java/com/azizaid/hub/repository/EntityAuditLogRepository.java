package com.azizaid.hub.repository;

import com.azizaid.hub.model.EntityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntityAuditLogRepository extends JpaRepository<EntityAuditLog, Long> {

    List<EntityAuditLog> findByFichaIdOrderByTimestampDesc(Long fichaId);
}
