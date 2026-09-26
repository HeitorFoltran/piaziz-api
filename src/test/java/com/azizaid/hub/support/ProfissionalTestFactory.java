package com.azizaid.hub.support;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;

import java.util.UUID;

public final class ProfissionalTestFactory {

    private ProfissionalTestFactory() {
    }

    public static String persistirComToken(ProfissionalRepository profissionalRepository,
                                            JwtService jwtService,
                                            PapelProfissional role) {
        String email = role.name().toLowerCase() + "." + UUID.randomUUID() + "@azizaidhub.local";
        Profissional profissional = profissionalRepository.save(Profissional.builder()
                .nome("Profissional Teste")
                .cpf("12345678900")
                .email(email)
                .senhaHash("hash-irrelevante-pro-teste")
                .role(role)
                .build());

        return jwtService.gerarToken(profissional.getId(), profissional.getEmail(), role);
    }
}
