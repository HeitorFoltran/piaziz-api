package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.CategoriaClassificacao;
import com.azizaid.hub.model.enums.FrequenciaViolencia;
import com.azizaid.hub.model.enums.TipoViolencia;
import jakarta.validation.constraints.Size;

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

        Boolean sugereSaudeGeral,

        @Size(max = 200, message = "sugereSaudeGeralQual deve ter no máximo 200 caracteres")
        String sugereSaudeGeralQual,

        Boolean sugereSaudeMental,

        @Size(max = 200, message = "sugereSaudeMentalQual deve ter no máximo 200 caracteres")
        String sugereSaudeMentalQual,

        Boolean sugereHabitacao,

        @Size(max = 200, message = "sugereHabitacaoQual deve ter no máximo 200 caracteres")
        String sugereHabitacaoQual,

        Boolean sugereTrabalhoEmprego,

        @Size(max = 200, message = "sugereTrabalhoEmpregoQual deve ter no máximo 200 caracteres")
        String sugereTrabalhoEmpregoQual,

        Boolean sugereAssistenciaSocial,

        @Size(max = 200, message = "sugereAssistenciaSocialQual deve ter no máximo 200 caracteres")
        String sugereAssistenciaSocialQual,

        Boolean sugereAssistenciaEducacional,

        @Size(max = 200, message = "sugereAssistenciaEducacionalQual deve ter no máximo 200 caracteres")
        String sugereAssistenciaEducacionalQual,

        @Size(max = 300, message = "sugereOutro deve ter no máximo 300 caracteres")
        String sugereOutro,

        String observacoesRelevantes,
        String responsavelAcolhimentoJuridico
) {
}
