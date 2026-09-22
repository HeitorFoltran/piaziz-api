package com.azizaid.hub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FichaPublicaRequestDTO(
        @NotBlank(message = "nome é obrigatório")
        String nome,

        @NotBlank(message = "cpf é obrigatório")
        String cpf,

        String telefone,
        Integer idade,
        String situacaoRelatada
) {
}
