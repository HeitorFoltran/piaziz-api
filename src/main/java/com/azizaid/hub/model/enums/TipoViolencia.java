package com.azizaid.hub.model.enums;

public enum TipoViolencia {
    FISICA("Física"),
    PSICOLOGICA("Psicológica"),
    MORAL("Moral"),
    PATRIMONIAL("Patrimonial"),
    OUTRA("Outra");

    private final String label;

    TipoViolencia(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
