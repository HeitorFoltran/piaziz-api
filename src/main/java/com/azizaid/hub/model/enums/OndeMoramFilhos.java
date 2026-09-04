package com.azizaid.hub.model.enums;

public enum OndeMoramFilhos {
    COMIGO("Comigo"),
    COM_FAMILIARES_AMIGOS("Com familiares/amigos"),
    ABRIGO_INSTITUCIONAL("Em abrigo institucional/família acolhedora"),
    SOZINHO_CONJUGE("Sozinho/cônjuge"),
    NAO_TEM_FILHOS("Não tenho filhos");

    private final String label;

    OndeMoramFilhos(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
