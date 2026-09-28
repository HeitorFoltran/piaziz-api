package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.AlteracaoFichaResponseDTO;
import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FichaAlteracaoService {

    // Não existe em AcaoAlteracao de propósito: a criação nunca é gravada no entity_audit_log, a linha
    // é montada na leitura a partir de ficha.criado_por_id e ficha.data_criacao.
    static final String ACAO_CRIOU = "CRIOU";

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
        Ficha ficha = fichaService.buscarEntidade(fichaId);
        List<EntityAuditLog> linhas = entityAuditLogRepository.findByFichaIdOrderByTimestampDesc(fichaId);

        List<Long> ids = Stream.of(
                        linhas.stream().map(EntityAuditLog::getEditorId),
                        linhas.stream().map(EntityAuditLog::getDonoId),
                        Stream.of(ficha.getCriadoPorId()))
                .flatMap(s -> s)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> nomesPorId = profissionalRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Profissional::getId, Profissional::getNome));

        List<AlteracaoFichaResponseDTO> resposta = new ArrayList<>(linhas.stream()
                .map(l -> new AlteracaoFichaResponseDTO(
                        l.getId(),
                        l.getTipoEntidade(),
                        // Linhas anteriores ao lote 5 não têm ação; todas eram edições.
                        (l.getAcao() != null ? l.getAcao() : AcaoAlteracao.EDITOU).name(),
                        l.getDetalhe(),
                        l.getEditorId(),
                        nomesPorId.get(l.getEditorId()),
                        l.getDonoId(),
                        nomesPorId.get(l.getDonoId()),
                        l.getTimestamp()))
                .toList());

        // A criação é sempre o evento mais antigo, então vai no fim da lista (ordem decrescente).
        if (ficha.getDataCriacao() != null) {
            Long criadorId = ficha.getCriadoPorId();
            String criadorNome = criadorId != null ? nomesPorId.get(criadorId) : null;
            resposta.add(new AlteracaoFichaResponseDTO(
                    null, "Ficha", ACAO_CRIOU, null,
                    criadorId, criadorNome, criadorId, criadorNome,
                    ficha.getDataCriacao()));
        }
        return resposta;
    }
}
