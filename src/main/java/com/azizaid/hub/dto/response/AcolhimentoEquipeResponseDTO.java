package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.AcolhimentoEquipe;
import com.azizaid.hub.model.enums.TipoViolencia;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

public record AcolhimentoEquipeResponseDTO(
        Long id,
        Long fichaId,
        String numeroProcessoMpu,
        LocalDate dataReuniaoAcolhimento,
        String servidorResponsavel,
        Set<String> tiposViolencia,
        String tipoViolenciaOutraDescricao,
        String frequenciaViolencia,
        Boolean medidasProtetivasAnteriores,
        String ameacasRelatadas,
        Boolean necessidadeAtendimentoMedicoImediato,
        Boolean acompanhamentoSaudeMentalEmCurso,
        String acompanhamentoSaudeMentalLocal,
        Boolean dependenteSofreuViolencia,
        Boolean dependentePrecisaAuxilioMedico,
        String categoriaClassificacao,
        String observacoesRelevantes,
        String responsavelAcolhimentoJuridico
) {
    public static AcolhimentoEquipeResponseDTO from(AcolhimentoEquipe a) {
        if (a == null) {
            return null;
        }
        Set<String> tipos = a.getTiposViolencia() == null ? Set.of()
                : a.getTiposViolencia().stream().map(TipoViolencia::name).collect(Collectors.toSet());
        return new AcolhimentoEquipeResponseDTO(
                a.getId(),
                a.getFicha() != null ? a.getFicha().getId() : null,
                a.getNumeroProcessoMpu(),
                a.getDataReuniaoAcolhimento(),
                a.getServidorResponsavel(),
                tipos,
                a.getTipoViolenciaOutraDescricao(),
                a.getFrequenciaViolencia() != null ? a.getFrequenciaViolencia().name() : null,
                a.getMedidasProtetivasAnteriores(),
                a.getAmeacasRelatadas(),
                a.getNecessidadeAtendimentoMedicoImediato(),
                a.getAcompanhamentoSaudeMentalEmCurso(),
                a.getAcompanhamentoSaudeMentalLocal(),
                a.getDependenteSofreuViolencia(),
                a.getDependentePrecisaAuxilioMedico(),
                a.getCategoriaClassificacao() != null ? a.getCategoriaClassificacao().name() : null,
                a.getObservacoesRelevantes(),
                a.getResponsavelAcolhimentoJuridico()
        );
    }
}
