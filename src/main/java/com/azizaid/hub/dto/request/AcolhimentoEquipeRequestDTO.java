package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.CategoriaClassificacao;
import com.azizaid.hub.model.enums.FrequenciaViolencia;
import com.azizaid.hub.model.enums.TipoViolencia;

import java.time.LocalDate;
import java.util.Set;

public record AcolhimentoEquipeRequestDTO(
        String numeroProcessoMpu,
        LocalDate dataReuniaoAcolhimento,
        String servidorResponsavel,
        Set<TipoViolencia> tiposViolencia,
        String tipoViolenciaOutraDescricao,
        FrequenciaViolencia frequenciaViolencia,
        Boolean medidasProtetivasAnteriores,
        String ameacasRelatadas,
        Boolean necessidadeAtendimentoMedicoImediato,
        Boolean acompanhamentoSaudeMentalEmCurso,
        String acompanhamentoSaudeMentalLocal,
        Boolean dependenteSofreuViolencia,
        Boolean dependentePrecisaAuxilioMedico,
        CategoriaClassificacao categoriaClassificacao,
        String observacoesRelevantes,
        String responsavelAcolhimentoJuridico
) {
}
