package com.azizaid.hub.model.enums;

public enum PapelProfissional {
    DEV("Desenvolvedor"),
    PADRAO("Padrão"),
    ESTAGIARIO("Estagiário");

    private final String label;

    PapelProfissional(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
