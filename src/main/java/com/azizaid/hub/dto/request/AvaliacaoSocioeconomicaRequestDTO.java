package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.NivelEscolaridade;
import com.azizaid.hub.model.enums.NivelEscrita;
import com.azizaid.hub.model.enums.PeriodoTrabalho;

import java.math.BigDecimal;

public record AvaliacaoSocioeconomicaRequestDTO(
        Boolean temRenda,
        BigDecimal valorRenda,
        Integer pessoasDependemRenda,
        String origemRenda,
        Boolean trabalhoFormal,
        Boolean rendaSuficiente,
        Boolean trabalhandoAtualmente,
        String ondeTrabalha,
        Boolean problemaSaudeAtrapalhaTrabalho,
        String problemaSaudeQual,
        Boolean situacaoFamiliarAtrapalhaTrabalho,
        String situacaoFamiliarQual,
        Boolean desejaTrabalhar,
        PeriodoTrabalho periodoDesejado,
        Boolean sabeLer,
        NivelEscrita nivelEscrita,
        NivelEscolaridade nivelEscolaridade,
        String escolaridadeDetalhe,
        Boolean fezCursoProfissionalizante,
        String cursoProfissionalizanteQual,
        Boolean desejaAuxilioCeebja,
        Boolean desejaCursoSenai,
        String areaCursoSenai,
        Boolean temRedeApoio,
        Boolean precisaAjudaMoradia,
        Boolean temOQueComer,
        Boolean acompanhamentoMedico,
        Boolean precisaAjudaTratamentoMedico,
        Boolean usoContinuoMedicamento,
        String medicamentoQuais,
        Boolean acessoMedicamentos
) {
}
