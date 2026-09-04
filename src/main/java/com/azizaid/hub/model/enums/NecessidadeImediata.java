package com.azizaid.hub.model.enums;

public enum NecessidadeImediata {
    INFORMACOES_DIREITOS("Informações sobre meus direitos"),
    ATENDIMENTO_SAUDE("Atendimento de saúde"),
    APOIO_PSICOLOGICO("Apoio psicológico"),
    ATENDIMENTO_ASSISTENCIA_SOCIAL("Atendimento da assistência social"),
    APOIO_MORADIA("Apoio para moradia"),
    APOIO_TRABALHO("Apoio para trabalho/emprego"),
    OUTRO("Outro");

    private final String label;

    NecessidadeImediata(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
