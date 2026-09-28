package com.azizaid.hub.dto.response;

import java.util.List;

// Página de uma listagem. Não expor o Page do Spring direto: o JSON do PageImpl não é estável
// entre versões.
public record PaginaDTO<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas
) {
    public static <T> PaginaDTO<T> de(List<T> itens, int pagina, int tamanho, long totalItens) {
        int totalPaginas = (int) ((totalItens + tamanho - 1) / tamanho);
        return new PaginaDTO<>(itens, pagina, tamanho, totalItens, totalPaginas);
    }
}
