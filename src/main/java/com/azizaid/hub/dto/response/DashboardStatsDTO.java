package com.azizaid.hub.dto.response;

import java.util.List;

public record DashboardStatsDTO(
        long totalFichasAtivas,
        long totalEncaminhamentos,
        long fichasAguardandoRetorno,
        long fichasSemAtualizacao30Dias,
        List<EncaminhamentoPorTipo> encaminhamentosPorTipo,
        List<FichasPorMes> fichasPorMes
) {

    public record EncaminhamentoPorTipo(
            String tipo,
            String label,
            long total
    ) {
    }

    public record FichasPorMes(
            String mes,
            int ano,
            long total
    ) {
    }
}
