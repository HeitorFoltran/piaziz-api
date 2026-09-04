package com.azizaid.hub.support;

import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.StatusFicha;

public final class FichaTestFactory {

    private FichaTestFactory() {
    }

    public static Ficha construirFichaComStatus(StatusFicha status) {
        return Ficha.builder().status(status).build();
    }

    public static FichaRequestDTO construirDtoValido(String cpf) {
        return new FichaRequestDTO(
                null,
                "Vítima Teste",
                cpf,
                30,
                "11999999999",
                "solteira",
                0,
                null,
                3,
                null,
                null,
                1,
                0,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
