package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.Size;

public record HistoricoAtendimentoRequestDTO(
        Boolean jaProcurouServico,

        @Size(max = 300, message = "servicoProcuradoQualOnde deve ter no máximo 300 caracteres")
        String servicoProcuradoQualOnde,

        Boolean emFilaEspera,

        @Size(max = 200, message = "filaEsperaQual deve ter no máximo 200 caracteres")
        String filaEsperaQual,

        Boolean jaPediuAjudaJusticaPolicia,

        @Size(max = 200, message = "justicaPoliciaQual deve ter no máximo 200 caracteres")
        String justicaPoliciaQual,

        @Size(max = 1000, message = "comoFoiAtendimento deve ter no máximo 1000 caracteres")
        String comoFoiAtendimento,

        Boolean resolveuSituacao,

        @Size(max = 500, message = "reacaoAgressor deve ter no máximo 500 caracteres")
        String reacaoAgressor
) {
}
