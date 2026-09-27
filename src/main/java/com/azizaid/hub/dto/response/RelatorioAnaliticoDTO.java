package com.azizaid.hub.dto.response;

import java.util.List;

/**
 * {@code progressaoCadastros}/{@code progressaoEncaminhamentos}/{@code progressaoAtendimentos}
 * points use one label format per call, chosen by the requested {@code granularidade}:
 * {@code "yyyy-MM-dd"} for dia/semana buckets, {@code "yyyy-MM"} for mes buckets.
 */
public record RelatorioAnaliticoDTO(
        long totalCadastros,
        long totalEncaminhamentos,
        double percentualCadastrosEncaminhados,
        double taxaConversaoEncaminhamentoAtendimento,
        Double tempoMedioCadastroAtendimentoDias,
        double mediaCadastrosPorMes,
        double mediaCadastrosPorSemana,
        double mediaCadastrosPorDia,
        double mediaEncaminhamentosPorMes,
        double mediaEncaminhamentosPorSemana,
        double mediaEncaminhamentosPorDia,
        List<PontoSerieDTO> progressaoCadastros,
        List<PontoSerieDTO> progressaoEncaminhamentos,
        List<PontoSerieDTO> progressaoAtendimentos
) {
}
