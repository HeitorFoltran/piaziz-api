package com.azizaid.hub.config;

import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Component
public class ConviteTokenService {

    private static final long EXPIRACAO_DIAS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConviteFichaRepository conviteFichaRepository;

    public ConviteTokenService(ConviteFichaRepository conviteFichaRepository) {
        this.conviteFichaRepository = conviteFichaRepository;
    }

    public String gerarTokenCru() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String tokenCru) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(tokenCru.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public LocalDateTime calcularExpiracao() {
        return LocalDateTime.now().plusDays(EXPIRACAO_DIAS);
    }

    @Transactional
    public ValidacaoTokenResult validar(String tokenCru) {
        Optional<ConviteFicha> encontrado = conviteFichaRepository.findByTokenHash(hash(tokenCru));
        if (encontrado.isEmpty()) {
            return ValidacaoTokenResult.invalido(MotivoTokenInvalido.INVALIDO);
        }

        ConviteFicha convite = encontrado.get();

        if (convite.getStatus() == StatusConvite.ATIVO && convite.getDataExpiracao().isBefore(LocalDateTime.now())) {
            convite.setStatus(StatusConvite.EXPIRADO);
            conviteFichaRepository.save(convite);
            return ValidacaoTokenResult.invalido(MotivoTokenInvalido.EXPIRADO);
        }

        if (convite.getStatus() == StatusConvite.EXPIRADO) {
            return ValidacaoTokenResult.invalido(MotivoTokenInvalido.EXPIRADO);
        }

        if (convite.getStatus() != StatusConvite.ATIVO) {
            return ValidacaoTokenResult.invalido(MotivoTokenInvalido.USADO);
        }

        return ValidacaoTokenResult.valido(convite);
    }

    public enum MotivoTokenInvalido {
        INVALIDO, EXPIRADO, USADO
    }

    public record ValidacaoTokenResult(boolean valido, ConviteFicha convite, MotivoTokenInvalido motivo) {
        public static ValidacaoTokenResult valido(ConviteFicha convite) {
            return new ValidacaoTokenResult(true, convite, null);
        }

        public static ValidacaoTokenResult invalido(MotivoTokenInvalido motivo) {
            return new ValidacaoTokenResult(false, null, motivo);
        }
    }
}
