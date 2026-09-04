package com.azizaid.hub.dto.request;

public record HistoricoAtendimentoRequestDTO(
        Boolean jaProcurouServico,
        String servicoProcuradoQualOnde,
        Boolean emFilaEspera,
        String filaEsperaQual,
        Boolean jaPediuAjudaJusticaPolicia,
        String justicaPoliciaQual,
        String comoFoiAtendimento,
        Boolean resolveuSituacao,
        String reacaoAgressor
) {
}
