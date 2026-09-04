package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.HistoricoAtendimento;

public record HistoricoAtendimentoResponseDTO(
        Long id,
        Long fichaId,
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
    public static HistoricoAtendimentoResponseDTO from(HistoricoAtendimento h) {
        if (h == null) {
            return null;
        }
        return new HistoricoAtendimentoResponseDTO(
                h.getId(),
                h.getFicha() != null ? h.getFicha().getId() : null,
                h.getJaProcurouServico(),
                h.getServicoProcuradoQualOnde(),
                h.getEmFilaEspera(),
                h.getFilaEsperaQual(),
                h.getJaPediuAjudaJusticaPolicia(),
                h.getJusticaPoliciaQual(),
                h.getComoFoiAtendimento(),
                h.getResolveuSituacao(),
                h.getReacaoAgressor()
        );
    }
}
