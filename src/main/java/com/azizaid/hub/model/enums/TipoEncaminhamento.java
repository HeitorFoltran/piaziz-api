package com.azizaid.hub.model.enums;

import java.text.Normalizer;

public enum TipoEncaminhamento {
    SAUDE_GERAL("Serviços de saúde em geral"),
    SAUDE_MENTAL("Serviço de saúde mental"),
    HABITACAO("Habitação"),
    TRABALHO_EMPREGO("Trabalho/emprego"),
    ASSISTENCIA_SOCIAL("Assistência social (CRAS/CREAS)"),
    ASSISTENCIA_EDUCACIONAL("Assistência educacional"),
    OUTRO("Outro encaminhamento");

    private final String label;

    TipoEncaminhamento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TipoEncaminhamento fromFlexible(String value) {
        if (value == null) {
            return null;
        }
        String norm = normalizar(value);
        if (norm.isEmpty() || norm.equals("ALL") || norm.equals("TODOS")) {
            return null;
        }
        for (TipoEncaminhamento tipo : values()) {
            if (normalizar(tipo.name()).equals(norm) || normalizar(tipo.label).equals(norm)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de encaminhamento inválido: " + value);
    }

    private static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.trim().toUpperCase().replaceAll("[\\s-]+", "_");
    }
}
