package com.azizaid.hub.model.enums;

public enum TipoMoradia {
    CASA_PROPRIA("Casa própria"),
    ALUGADA("Casa alugada"),
    CEDIDA("Casa cedida"),
    ABRIGO("Abrigo temporário"),
    OUTRO("Outro");

    private final String label;

    TipoMoradia(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
