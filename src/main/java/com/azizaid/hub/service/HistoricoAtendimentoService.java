package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.HistoricoAtendimentoResponseDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.HistoricoAtendimento;
import com.azizaid.hub.repository.HistoricoAtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricoAtendimentoService {

    private final HistoricoAtendimentoRepository historicoAtendimentoRepository;
    private final FichaService fichaService;
    private final EntityAuditService entityAuditService;

    public HistoricoAtendimentoService(HistoricoAtendimentoRepository historicoAtendimentoRepository,
                                       FichaService fichaService,
                                       EntityAuditService entityAuditService) {
        this.historicoAtendimentoRepository = historicoAtendimentoRepository;
        this.fichaService = fichaService;
        this.entityAuditService = entityAuditService;
    }

    @Transactional(readOnly = true)
    public HistoricoAtendimentoResponseDTO buscarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        return historicoAtendimentoRepository.findByFichaId(fichaId)
                .map(HistoricoAtendimentoResponseDTO::from)
                .orElse(null);
    }

    @Transactional
    public HistoricoAtendimentoResponseDTO salvar(Long fichaId, HistoricoAtendimentoRequestDTO dto) {
        Ficha ficha = fichaService.buscarEntidade(fichaId);
        HistoricoAtendimento historico = historicoAtendimentoRepository.findByFichaId(fichaId)
                .orElseGet(() -> {
                    HistoricoAtendimento novo = new HistoricoAtendimento();
                    novo.setFicha(ficha);
                    return novo;
                });

        if (historico.getId() != null) {
            Long donoId = historico.getCriadoPorId() != null ? historico.getCriadoPorId() : historico.getUltimoEditorId();
            entityAuditService.registrarSeCrossUser("HistoricoAtendimento", historico.getId(), donoId);
        }

        historico.setJaProcurouServico(dto.jaProcurouServico());
        historico.setServicoProcuradoQualOnde(dto.servicoProcuradoQualOnde());
        historico.setEmFilaEspera(dto.emFilaEspera());
        historico.setFilaEsperaQual(dto.filaEsperaQual());
        historico.setJaPediuAjudaJusticaPolicia(dto.jaPediuAjudaJusticaPolicia());
        historico.setJusticaPoliciaQual(dto.justicaPoliciaQual());
        historico.setComoFoiAtendimento(dto.comoFoiAtendimento());
        historico.setResolveuSituacao(dto.resolveuSituacao());
        historico.setReacaoAgressor(dto.reacaoAgressor());

        return HistoricoAtendimentoResponseDTO.from(historicoAtendimentoRepository.save(historico));
    }
}
