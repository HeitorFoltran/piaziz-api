package com.azizaid.hub.dto.response;

import java.time.LocalDateTime;

public record AlteracaoFichaResponseDTO(
        Long id,
        String tipoEntidade,
        Long editorId,
        String editorNome,
        Long donoId,
        String donoNome,
        LocalDateTime timestamp
) {
}
