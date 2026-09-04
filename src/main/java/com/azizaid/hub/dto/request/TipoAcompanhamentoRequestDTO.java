package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoAcompanhamentoRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        String nome
) {
}
