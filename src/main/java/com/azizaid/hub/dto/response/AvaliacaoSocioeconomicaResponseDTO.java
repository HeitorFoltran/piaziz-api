package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.AvaliacaoSocioeconomica;

import java.math.BigDecimal;

public record AvaliacaoSocioeconomicaResponseDTO(
        Long id,
        Long fichaId,
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
        String periodoDesejado,
        Boolean sabeLer,
        String nivelEscrita,
        String nivelEscolaridade,
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
    public static AvaliacaoSocioeconomicaResponseDTO from(AvaliacaoSocioeconomica a) {
        if (a == null) {
            return null;
        }
        return new AvaliacaoSocioeconomicaResponseDTO(
                a.getId(),
                a.getFicha() != null ? a.getFicha().getId() : null,
                a.getTemRenda(),
                a.getValorRenda(),
                a.getPessoasDependemRenda(),
                a.getOrigemRenda(),
                a.getTrabalhoFormal(),
                a.getRendaSuficiente(),
                a.getTrabalhandoAtualmente(),
                a.getOndeTrabalha(),
                a.getProblemaSaudeAtrapalhaTrabalho(),
                a.getProblemaSaudeQual(),
                a.getSituacaoFamiliarAtrapalhaTrabalho(),
                a.getSituacaoFamiliarQual(),
                a.getDesejaTrabalhar(),
                a.getPeriodoDesejado() != null ? a.getPeriodoDesejado().name() : null,
                a.getSabeLer(),
                a.getNivelEscrita() != null ? a.getNivelEscrita().name() : null,
                a.getNivelEscolaridade() != null ? a.getNivelEscolaridade().name() : null,
                a.getEscolaridadeDetalhe(),
                a.getFezCursoProfissionalizante(),
                a.getCursoProfissionalizanteQual(),
                a.getDesejaAuxilioCeebja(),
                a.getDesejaCursoSenai(),
                a.getAreaCursoSenai(),
                a.getTemRedeApoio(),
                a.getPrecisaAjudaMoradia(),
                a.getTemOQueComer(),
                a.getAcompanhamentoMedico(),
                a.getPrecisaAjudaTratamentoMedico(),
                a.getUsoContinuoMedicamento(),
                a.getMedicamentoQuais(),
                a.getAcessoMedicamentos()
        );
    }
}
