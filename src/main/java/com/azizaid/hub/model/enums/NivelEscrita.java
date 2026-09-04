package com.azizaid.hub.model.enums;

public enum NivelEscrita {
    NAO("Não"),
    SO_NOME("Sei escrever meu nome"),
    SIM("Sim");

    private final String label;

    NivelEscrita(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
