package com.azizaid.hub.model.enums;

public enum StatusFicha {
    ATIVO("Ativo"),
    PAUSADO("Pausado"),
    ENCERRADO("Arquivado");

    private final String label;

    StatusFicha(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
