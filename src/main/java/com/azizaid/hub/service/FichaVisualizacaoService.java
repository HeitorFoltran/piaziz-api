package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.VisualizacaoFichaResponseDTO;
import com.azizaid.hub.model.LeituraAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.LeituraAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Separado do LeituraAuditService porque precisa do FichaService (404), e o FichaService depende
// do LeituraAuditService.
@Service
public class FichaVisualizacaoService {

    private final FichaService fichaService;
    private final LeituraAuditLogRepository leituraAuditLogRepository;
    private final ProfissionalRepository profissionalRepository;

    public FichaVisualizacaoService(FichaService fichaService,
                                    LeituraAuditLogRepository leituraAuditLogRepository,
                                    ProfissionalRepository profissionalRepository) {
        this.fichaService = fichaService;
        this.leituraAuditLogRepository = leituraAuditLogRepository;
        this.profissionalRepository = profissionalRepository;
    }

    // Consultar o log não conta como leitura do caso.
    @Transactional(readOnly = true)
    public List<VisualizacaoFichaResponseDTO> listarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        List<LeituraAuditLog> linhas = leituraAuditLogRepository.findTop200ByFichaIdOrderByTimestampDescIdDesc(fichaId);

        List<Long> ids = linhas.stream().map(LeituraAuditLog::getProfissionalId).distinct().toList();
        Map<Long, String> nomesPorId = profissionalRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Profissional::getId, Profissional::getNome));

        return linhas.stream()
                .map(l -> new VisualizacaoFichaResponseDTO(
                        l.getProfissionalId(),
                        nomesPorId.get(l.getProfissionalId()),
                        l.getTimestamp()))
                .toList();
    }
}
