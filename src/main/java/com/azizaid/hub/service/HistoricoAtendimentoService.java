package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.HistoricoAtendimentoResponseDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.HistoricoAtendimento;
import com.azizaid.hub.repository.HistoricoAtendimentoRepository;
import com.azizaid.hub.util.RetratoAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        boolean novo = historico.getId() == null;
        List<Object> antes = retrato(historico);

        historico.setJaProcurouServico(dto.jaProcurouServico());
        historico.setServicoProcuradoQualOnde(dto.servicoProcuradoQualOnde());
        historico.setEmFilaEspera(dto.emFilaEspera());
        historico.setFilaEsperaQual(dto.filaEsperaQual());
        historico.setJaPediuAjudaJusticaPolicia(dto.jaPediuAjudaJusticaPolicia());
        historico.setJusticaPoliciaQual(dto.justicaPoliciaQual());
        historico.setComoFoiAtendimento(dto.comoFoiAtendimento());
        historico.setResolveuSituacao(dto.resolveuSituacao());
        historico.setReacaoAgressor(dto.reacaoAgressor());

        HistoricoAtendimento salvo = historicoAtendimentoRepository.save(historico);
        Long donoId = salvo.getCriadoPorId() != null ? salvo.getCriadoPorId() : salvo.getUltimoEditorId();
        entityAuditService.registrarSalvamentoDeParte("HistoricoAtendimento", salvo.getId(), donoId, ficha,
                novo, antes, retrato(salvo));
        return HistoricoAtendimentoResponseDTO.from(salvo);
    }

    // Campos gravados por salvar(). Ele não mexe em nenhum campo da ficha.
    private List<Object> retrato(HistoricoAtendimento h) {
        return RetratoAuditoria.de(h.getJaProcurouServico(), h.getServicoProcuradoQualOnde(), h.getEmFilaEspera(),
                h.getFilaEsperaQual(), h.getJaPediuAjudaJusticaPolicia(), h.getJusticaPoliciaQual(),
                h.getComoFoiAtendimento(), h.getResolveuSituacao(), h.getReacaoAgressor());
    }
}
