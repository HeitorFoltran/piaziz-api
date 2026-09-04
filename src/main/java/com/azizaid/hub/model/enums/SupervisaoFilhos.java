package com.azizaid.hub.model.enums;

public enum SupervisaoFilhos {
    SIM("Sim"),
    NAO("Não"),
    NAO_PRECISA("Eles não precisam de supervisão");

    private final String label;

    SupervisaoFilhos(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
