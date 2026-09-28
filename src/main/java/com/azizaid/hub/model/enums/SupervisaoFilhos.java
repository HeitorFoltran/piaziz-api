package com.azizaid.hub.model.enums;

// A pergunta é "tenho com quem deixar meus filhos?" (item 8 do PIA). Antes do lote 7 o front perguntava
// "precisam de supervisão?": SIM/NAO gravados antes disso podem ter o sentido invertido.
public enum SupervisaoFilhos {
    SIM("Sim, tenho com quem deixar"),
    NAO("Não tenho com quem deixar"),
    NAO_PRECISA("Eles não precisam de supervisão");

    private final String label;

    SupervisaoFilhos(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
