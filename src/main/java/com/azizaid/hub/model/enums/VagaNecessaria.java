package com.azizaid.hub.model.enums;

public enum VagaNecessaria {
    CRECHE("Creche"),
    ESCOLA("Escola"),
    ATENDIMENTO_PSICOSSOCIAL("Atendimento Psicossocial"),
    NAO_PRECISA("Não precisa de atendimento");

    private final String label;

    VagaNecessaria(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
