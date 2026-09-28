package com.azizaid.hub.dto.response;

import java.time.LocalDateTime;

// id é null na linha sintética de criação do caso (acao = "CRIOU"), que não vem do log.
public record AlteracaoFichaResponseDTO(
        Long id,
        String tipoEntidade,
        String acao,
        String detalhe,
        Long editorId,
        String editorNome,
        Long donoId,
        String donoNome,
        LocalDateTime timestamp
) {
}
