package com.azizaid.hub.model.enums;

public enum NivelEscolaridade {
    NAO_ESTUDOU("Não estudei"),
    FUNDAMENTAL("Ensino Fundamental"),
    MEDIO("Ensino Médio"),
    TECNICO("Ensino Técnico"),
    FACULDADE("Faculdade");

    private final String label;

    NivelEscolaridade(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
