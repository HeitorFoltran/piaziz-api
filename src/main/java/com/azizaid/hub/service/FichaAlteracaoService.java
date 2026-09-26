package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.AlteracaoFichaResponseDTO;
import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FichaAlteracaoService {

    private final FichaService fichaService;
    private final EntityAuditLogRepository entityAuditLogRepository;
    private final ProfissionalRepository profissionalRepository;

    public FichaAlteracaoService(FichaService fichaService,
                                  EntityAuditLogRepository entityAuditLogRepository,
                                  ProfissionalRepository profissionalRepository) {
        this.fichaService = fichaService;
        this.entityAuditLogRepository = entityAuditLogRepository;
        this.profissionalRepository = profissionalRepository;
    }

    @Transactional(readOnly = true)
    public List<AlteracaoFichaResponseDTO> listarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        List<EntityAuditLog> linhas = entityAuditLogRepository.findByFichaIdOrderByTimestampDesc(fichaId);

        List<Long> ids = Stream.concat(
                        linhas.stream().map(EntityAuditLog::getEditorId),
                        linhas.stream().map(EntityAuditLog::getDonoId))
                .distinct()
                .toList();
        Map<Long, String> nomesPorId = profissionalRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Profissional::getId, Profissional::getNome));

        return linhas.stream()
                .map(l -> new AlteracaoFichaResponseDTO(
                        l.getId(),
                        l.getTipoEntidade(),
                        l.getEditorId(),
                        nomesPorId.get(l.getEditorId()),
                        l.getDonoId(),
                        nomesPorId.get(l.getDonoId()),
                        l.getTimestamp()))
                .toList();
    }
}
