package com.azizaid.hub.support;

import com.azizaid.hub.dto.request.FichaRequestDTO;

public final class FichaTestFactory {

    private FichaTestFactory() {
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
