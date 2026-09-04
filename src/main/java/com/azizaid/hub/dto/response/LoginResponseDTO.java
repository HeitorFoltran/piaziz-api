package com.azizaid.hub.dto.response;

public record LoginResponseDTO(
        String token,
        Long profissionalId,
        String nome,
        String email,
        String role
) {
}
