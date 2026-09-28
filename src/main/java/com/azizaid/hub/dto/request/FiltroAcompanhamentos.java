package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.StatusFicha;

import java.time.LocalDate;

// Filtros de GET /api/acompanhamentos. Todos opcionais; de/ate são inclusivos e valem sobre campoData.
public record FiltroAcompanhamentos(
        String q,
        Long servicoId,
        StatusFicha status,
        Long tipoId,
        boolean meus,
        CampoData campoData,
        LocalDate de,
        LocalDate ate
) {
    public FiltroAcompanhamentos {
        if (campoData == null) {
            campoData = CampoData.DATA_ATUALIZACAO;
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            throw new IllegalArgumentException("de não pode ser depois de ate");
        }
    }

    public enum CampoData {
        DATA_ATUALIZACAO("dataAtualizacao"),
        DATA_CRIACAO("dataCriacao");

        private final String parametro;

        CampoData(String parametro) {
            this.parametro = parametro;
        }

        public String getParametro() {
            return parametro;
        }

        // Ausente ou em branco vira o padrão, dataAtualizacao.
        public static CampoData fromParametro(String valor) {
            if (valor == null || valor.isBlank()) {
                return DATA_ATUALIZACAO;
            }
            for (CampoData campo : values()) {
                if (campo.parametro.equals(valor.trim())) {
                    return campo;
                }
            }
            throw new IllegalArgumentException("campoData deve ser dataAtualizacao ou dataCriacao");
        }
    }
}
