package com.azizaid.hub.model.enums;

public enum StatusFichaPendente {
    PENDENTE("Pendente"),
    APROVADA("Aprovada"),
    REJEITADA("Rejeitada");

    private final String label;

    StatusFichaPendente(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
