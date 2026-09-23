package com.azizaid.hub.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FichaPublicaRequestDTO(
        @Valid
        @NotNull(message = "ficha é obrigatória")
        FichaRequestDTO ficha,

        @Valid
        AvaliacaoSocioeconomicaRequestDTO avaliacao,

        @Valid
        HistoricoAtendimentoRequestDTO historico,

        @Size(max = 2000, message = "situacaoRelatada deve ter no máximo 2000 caracteres")
        String situacaoRelatada
) {
}
