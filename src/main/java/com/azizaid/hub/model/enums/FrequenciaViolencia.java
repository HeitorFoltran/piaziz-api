package com.azizaid.hub.model.enums;

public enum FrequenciaViolencia {
    EPISODIO_UNICO("Episódio único"),
    RECORRENTE("Recorrente");

    private final String label;

    FrequenciaViolencia(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
