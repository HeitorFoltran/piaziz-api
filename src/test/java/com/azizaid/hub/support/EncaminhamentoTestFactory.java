package com.azizaid.hub.support;

import com.azizaid.hub.dto.request.EncaminhamentoRequestDTO;

import java.time.LocalDate;

public final class EncaminhamentoTestFactory {

    private EncaminhamentoTestFactory() {
    }

    public static EncaminhamentoRequestDTO construirEncaminhamentoDtoValido(Long servicoId) {
        return new EncaminhamentoRequestDTO(
                servicoId,
                null,
                "Profissional Teste",
                LocalDate.now(),
                null,
                "Encaminhamento de teste"
        );
    }
}
