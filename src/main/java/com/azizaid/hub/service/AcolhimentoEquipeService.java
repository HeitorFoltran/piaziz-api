package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.AcolhimentoEquipeRequestDTO;
import com.azizaid.hub.dto.response.AcolhimentoEquipeResponseDTO;
import com.azizaid.hub.model.AcolhimentoEquipe;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.repository.AcolhimentoEquipeRepository;
import com.azizaid.hub.util.RetratoAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

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

        boolean novo = acolhimento.getId() == null;
        List<Object> antes = retrato(acolhimento);

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

        AcolhimentoEquipe salvo = acolhimentoEquipeRepository.save(acolhimento);
        Long donoId = salvo.getCriadoPorId() != null ? salvo.getCriadoPorId() : salvo.getUltimoEditorId();
        entityAuditService.registrarSalvamentoDeParte("AcolhimentoEquipe", salvo.getId(), donoId, ficha,
                novo, antes, retrato(salvo));
        return AcolhimentoEquipeResponseDTO.from(salvo);
    }

    // Campos gravados por salvar(). Ele não mexe em nenhum campo da ficha.
    private List<Object> retrato(AcolhimentoEquipe a) {
        return RetratoAuditoria.de(a.getNumeroProcessoMpu(), a.getDataReuniaoAcolhimento(),
                a.getServidorResponsavel(), a.getTiposViolencia(), a.getTipoViolenciaOutraDescricao(),
                a.getFrequenciaViolencia(), a.getMedidasProtetivasAnteriores(), a.getAmeacasRelatadas(),
                a.getNecessidadeAtendimentoMedicoImediato(), a.getAcompanhamentoSaudeMentalEmCurso(),
                a.getAcompanhamentoSaudeMentalLocal(), a.getDependenteSofreuViolencia(),
                a.getDependentePrecisaAuxilioMedico(), a.getCategoriaClassificacao(), a.getObservacoesRelevantes(),
                a.getResponsavelAcolhimentoJuridico());
    }
}
