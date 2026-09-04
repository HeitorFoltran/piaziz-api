package com.azizaid.hub.config;

import com.azizaid.hub.model.enums.PapelProfissional;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtService {

    private static final long EXPIRACAO_HORAS = 4;

    private final SecretKey chave;

    public JwtService(@Value("${JWT_SECRET}") String secret) {
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Long profissionalId, String email, PapelProfissional role) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(profissionalId))
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(EXPIRACAO_HORAS, ChronoUnit.HOURS)))
                .signWith(chave)
                .compact();
    }

    public Claims validarEExtrairClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extrairProfissionalId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public PapelProfissional extrairRole(Claims claims) {
        return PapelProfissional.valueOf(claims.get("role", String.class));
    }
}
