package com.azizaid.hub.model.enums;

public enum StatusConvite {
    ATIVO("Ativo"),
    USADO("Usado"),
    EXPIRADO("Expirado"),
    CANCELADO("Cancelado");

    private final String label;

    StatusConvite(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
