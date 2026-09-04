package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntityAuditService {

    private final EntityAuditLogRepository entityAuditLogRepository;

    public EntityAuditService(EntityAuditLogRepository entityAuditLogRepository) {
        this.entityAuditLogRepository = entityAuditLogRepository;
    }

    @Transactional
    public void registrarSeCrossUser(String tipoEntidade, Long entidadeId, Long donoId) {
        if (donoId == null) {
            return;
        }
        Long editorId = CurrentUser.id().orElse(null);
        if (editorId == null || editorId.equals(donoId)) {
            return;
        }
        entityAuditLogRepository.save(EntityAuditLog.builder()
                .tipoEntidade(tipoEntidade)
                .entidadeId(entidadeId)
                .editorId(editorId)
                .donoId(donoId)
                .resumo("Edição cross-user: profissional " + editorId + " editou " + tipoEntidade
                        + " #" + entidadeId + " (dono: " + donoId + ")")
                .build());
    }
}
