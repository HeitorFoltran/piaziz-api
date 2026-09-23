package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.NivelEscolaridade;
import com.azizaid.hub.model.enums.NivelEscrita;
import com.azizaid.hub.model.enums.PeriodoTrabalho;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AvaliacaoSocioeconomicaRequestDTO(
        Boolean temRenda,
        BigDecimal valorRenda,
        Integer pessoasDependemRenda,

        @Size(max = 200, message = "origemRenda deve ter no máximo 200 caracteres")
        String origemRenda,

        Boolean trabalhoFormal,
        Boolean rendaSuficiente,
        Boolean trabalhandoAtualmente,

        @Size(max = 200, message = "ondeTrabalha deve ter no máximo 200 caracteres")
        String ondeTrabalha,

        Boolean problemaSaudeAtrapalhaTrabalho,

        @Size(max = 200, message = "problemaSaudeQual deve ter no máximo 200 caracteres")
        String problemaSaudeQual,

        Boolean situacaoFamiliarAtrapalhaTrabalho,

        @Size(max = 200, message = "situacaoFamiliarQual deve ter no máximo 200 caracteres")
        String situacaoFamiliarQual,

        Boolean desejaTrabalhar,
        PeriodoTrabalho periodoDesejado,
        Boolean sabeLer,
        NivelEscrita nivelEscrita,
        NivelEscolaridade nivelEscolaridade,

        @Size(max = 150, message = "escolaridadeDetalhe deve ter no máximo 150 caracteres")
        String escolaridadeDetalhe,

        Boolean fezCursoProfissionalizante,

        @Size(max = 200, message = "cursoProfissionalizanteQual deve ter no máximo 200 caracteres")
        String cursoProfissionalizanteQual,

        Boolean desejaAuxilioCeebja,
        Boolean desejaCursoSenai,

        @Size(max = 150, message = "areaCursoSenai deve ter no máximo 150 caracteres")
        String areaCursoSenai,

        Boolean temRedeApoio,
        Boolean precisaAjudaMoradia,
        Boolean temOQueComer,
        Boolean acompanhamentoMedico,
        Boolean precisaAjudaTratamentoMedico,
        Boolean usoContinuoMedicamento,

        @Size(max = 300, message = "medicamentoQuais deve ter no máximo 300 caracteres")
        String medicamentoQuais,

        Boolean acessoMedicamentos
) {
}
