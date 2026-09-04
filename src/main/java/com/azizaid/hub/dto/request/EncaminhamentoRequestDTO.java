package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.TipoEncaminhamento;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EncaminhamentoRequestDTO(
        @NotNull(message = "servicoId é obrigatório")
        Long servicoId,
        TipoEncaminhamento categoria,
        String profissional,
        LocalDate dataEncaminhamento,
        LocalDate dataRetorno,
        String descricao
) {
}