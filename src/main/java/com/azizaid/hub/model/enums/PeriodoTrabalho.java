package com.azizaid.hub.model.enums;

public enum PeriodoTrabalho {
    DIA("Durante o dia"),
    NOITE("Durante a noite"),
    MEIO_PERIODO("Meio período");

    private final String label;

    PeriodoTrabalho(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
