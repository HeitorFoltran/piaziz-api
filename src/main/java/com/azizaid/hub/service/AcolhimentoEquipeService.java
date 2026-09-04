package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.AcolhimentoEquipeRequestDTO;
import com.azizaid.hub.dto.response.AcolhimentoEquipeResponseDTO;
import com.azizaid.hub.model.AcolhimentoEquipe;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.repository.AcolhimentoEquipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class AcolhimentoEquipeService {

    private final AcolhimentoEquipeRepository acolhimentoEquipeRepository;
    private final FichaService fichaService;
    private final EntityAuditService entityAuditService;

    public AcolhimentoEquipeService(AcolhimentoEquipeRepository acolhimentoEquipeRepository,
                                    FichaService fichaService,
                                    EntityAuditService entityAuditService) {
        this.acolhimentoEquipeRepository = acolhimentoEquipeRepository;
        this.fichaService = fichaService;
        this.entityAuditService = entityAuditService;
    }

    @Transactional(readOnly = true)
    public AcolhimentoEquipeResponseDTO buscarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        return acolhimentoEquipeRepository.findByFichaId(fichaId)
                .map(AcolhimentoEquipeResponseDTO::from)
                .orElse(null);
    }

    @Transactional
    public AcolhimentoEquipeResponseDTO salvar(Long fichaId, AcolhimentoEquipeRequestDTO dto) {
        Ficha ficha = fichaService.buscarEntidade(fichaId);
        AcolhimentoEquipe acolhimento = acolhimentoEquipeRepository.findByFichaId(fichaId)
                .orElseGet(() -> {
                    AcolhimentoEquipe novo = new AcolhimentoEquipe();
                    novo.setFicha(ficha);
                    return novo;
                });

        if (acolhimento.getId() != null) {
            Long donoId = acolhimento.getCriadoPorId() != null ? acolhimento.getCriadoPorId() : acolhimento.getUltimoEditorId();
            entityAuditService.registrarSeCrossUser("AcolhimentoEquipe", acolhimento.getId(), donoId);
        }

        acolhimento.setNumeroProcessoMpu(dto.numeroProcessoMpu());
        acolhimento.setDataReuniaoAcolhimento(dto.dataReuniaoAcolhimento());
        acolhimento.setServidorResponsavel(dto.servidorResponsavel());
        acolhimento.setTiposViolencia(dto.tiposViolencia() != null ? dto.tiposViolencia() : new HashSet<>());
        acolhimento.setTipoViolenciaOutraDescricao(dto.tipoViolenciaOutraDescricao());
        acolhimento.setFrequenciaViolencia(dto.frequenciaViolencia());
        acolhimento.setMedidasProtetivasAnteriores(dto.medidasProtetivasAnteriores());
        acolhimento.setAmeacasRelatadas(dto.ameacasRelatadas());
        acolhimento.setNecessidadeAtendimentoMedicoImediato(dto.necessidadeAtendimentoMedicoImediato());
        acolhimento.setAcompanhamentoSaudeMentalEmCurso(dto.acompanhamentoSaudeMentalEmCurso());
        acolhimento.setAcompanhamentoSaudeMentalLocal(dto.acompanhamentoSaudeMentalLocal());
        acolhimento.setDependenteSofreuViolencia(dto.dependenteSofreuViolencia());
        acolhimento.setDependentePrecisaAuxilioMedico(dto.dependentePrecisaAuxilioMedico());
        acolhimento.setCategoriaClassificacao(dto.categoriaClassificacao());
        acolhimento.setObservacoesRelevantes(dto.observacoesRelevantes());
        acolhimento.setResponsavelAcolhimentoJuridico(dto.responsavelAcolhimentoJuridico());

        return AcolhimentoEquipeResponseDTO.from(acolhimentoEquipeRepository.save(acolhimento));
    }
}
