package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.TipoAcompanhamento;

// Item da listagem do catálogo (aba Cadastros e selects). Separado de TipoAcompanhamentoResponseDTO,
// que vai dentro de ficha e acompanhamento, onde emUso não faz sentido.
// emUso: atribuído a pelo menos um caso, então não pode ser excluído.
public record TipoAcompanhamentoCadastroDTO(Long id, String nome, boolean emUso) {
    public static TipoAcompanhamentoCadastroDTO from(TipoAcompanhamento t, boolean emUso) {
        return new TipoAcompanhamentoCadastroDTO(t.getId(), t.getNome(), emUso);
    }
}
