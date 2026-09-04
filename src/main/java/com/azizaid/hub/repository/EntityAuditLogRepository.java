package com.azizaid.hub.repository;

import com.azizaid.hub.model.EntityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntityAuditLogRepository extends JpaRepository<EntityAuditLog, Long> {
}
