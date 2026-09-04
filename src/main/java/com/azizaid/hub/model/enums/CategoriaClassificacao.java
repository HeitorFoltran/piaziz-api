package com.azizaid.hub.model.enums;

public enum CategoriaClassificacao {
    CATEGORIA_1("Mulheres com trabalho adequado: apenas informações necessárias"),
    CATEGORIA_2("Mulheres sem trabalho adequado, mas com condições de trabalhar"),
    CATEGORIA_3("Mulheres sem trabalho adequado e sem condições de trabalhar"),
    CATEGORIA_4("Mulheres vítimas de crimes sexuais");

    private final String label;

    CategoriaClassificacao(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
