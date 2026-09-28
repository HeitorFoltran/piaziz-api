package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.CategoriaClassificacao;
import com.azizaid.hub.model.enums.FrequenciaViolencia;
import com.azizaid.hub.model.enums.TipoViolencia;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record AcolhimentoEquipeRequestDTO(
        @Size(max = 40, message = "numeroProcessoMpu deve ter no máximo 40 caracteres")
        String numeroProcessoMpu,

        LocalDate dataReuniaoAcolhimento,

        @Size(max = 150, message = "servidorResponsavel deve ter no máximo 150 caracteres")
        String servidorResponsavel,

        Set<TipoViolencia> tiposViolencia,

        @Size(max = 200, message = "tipoViolenciaOutraDescricao deve ter no máximo 200 caracteres")
        String tipoViolenciaOutraDescricao,

        FrequenciaViolencia frequenciaViolencia,
        Boolean medidasProtetivasAnteriores,

        @Size(max = 1000, message = "ameacasRelatadas deve ter no máximo 1000 caracteres")
        String ameacasRelatadas,

        Boolean necessidadeAtendimentoMedicoImediato,
        Boolean acompanhamentoSaudeMentalEmCurso,

        @Size(max = 150, message = "acompanhamentoSaudeMentalLocal deve ter no máximo 150 caracteres")
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

        @Size(max = 2000, message = "observacoesRelevantes deve ter no máximo 2000 caracteres")
        String observacoesRelevantes,

        @Size(max = 150, message = "responsavelAcolhimentoJuridico deve ter no máximo 150 caracteres")
        String responsavelAcolhimentoJuridico
) {
}
