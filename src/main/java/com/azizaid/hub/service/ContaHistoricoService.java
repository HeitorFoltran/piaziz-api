package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.ContaHistoricoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ContaAuditLog;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.ContaAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Leitura da conta_audit_log. Quem chega aqui já passou por @permissoes.podeGerenciarProfissionais(),
// inclusive para contas DEV: é só leitura, e o detalhe nunca contém senha nem hash.
@Service
public class ContaHistoricoService {

    private final ContaAuditLogRepository contaAuditLogRepository;
    private final ProfissionalRepository profissionalRepository;

    public ContaHistoricoService(ContaAuditLogRepository contaAuditLogRepository,
                                 ProfissionalRepository profissionalRepository) {
        this.contaAuditLogRepository = contaAuditLogRepository;
        this.profissionalRepository = profissionalRepository;
    }

    @Transactional(readOnly = true)
    public List<ContaHistoricoResponseDTO> listarPorProfissional(Long profissionalId) {
        if (!profissionalRepository.existsById(profissionalId)) {
            throw RecursoNaoEncontradoException.de("Profissional", profissionalId);
        }
        List<ContaAuditLog> linhas =
                contaAuditLogRepository.findByProfissionalIdOrderByTimestampDescIdDesc(profissionalId);

        List<Long> autorIds = linhas.stream().map(ContaAuditLog::getAutorId).distinct().toList();
        Map<Long, String> nomesPorId = profissionalRepository.findAllById(autorIds).stream()
                .collect(Collectors.toMap(Profissional::getId, Profissional::getNome));

        return linhas.stream()
                .map(l -> new ContaHistoricoResponseDTO(
                        l.getId(),
                        l.getAcao(),
                        l.getDetalhe(),
                        l.getAutorId(),
                        nomesPorId.get(l.getAutorId()),
                        l.getTimestamp()))
                .toList();
    }
}
