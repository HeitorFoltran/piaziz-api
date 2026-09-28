package com.azizaid.hub.config;

import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
    private static final Logger log = LoggerFactory.getLogger(ConviteTokenService.class);

    private static final String CIFRA = "AES/GCM/NoPadding";
    private static final int TAMANHO_CHAVE_BYTES = 32;
    private static final int TAMANHO_IV_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final ConviteFichaRepository conviteFichaRepository;
    private final SecretKey chaveCifra;

    // A chave só serve para mostrar o link de novo na lista. A validação do link público é pelo
    // hash, então trocar a chave não invalida nenhum link: ele só deixa de aparecer na lista.
    public ConviteTokenService(ConviteFichaRepository conviteFichaRepository,
                               @Value("${CONVITE_TOKEN_KEY}") String chaveBase64) {
        this.conviteFichaRepository = conviteFichaRepository;
        this.chaveCifra = decodificarChave(chaveBase64);
    }

    // A mensagem nunca leva o valor da chave, nem a exceção do decoder como causa (ela cita o
    // caractere inválido).
    private static SecretKey decodificarChave(String chaveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(chaveBase64.trim());
        } catch (IllegalArgumentException e) {
            bytes = new byte[0];
        }
        if (bytes.length != TAMANHO_CHAVE_BYTES) {
            throw new IllegalStateException("CONVITE_TOKEN_KEY precisa ter 32 bytes em Base64");
        }
        return new SecretKeySpec(bytes, "AES");
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

    // Devolve Base64(iv || ciphertext + tag). IV novo a cada chamada: repetir IV com a mesma chave
    // quebra o GCM.
    public String cifrar(String tokenCru) {
        byte[] iv = new byte[TAMANHO_IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIFRA);
            cipher.init(Cipher.ENCRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(tokenCru.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o token do convite", e);
        }
    }

    // Vazio quando não há o que mostrar: coluna NULL (convite anterior ao token cifrado ou que já
    // deixou de valer), valor corrompido ou chave trocada. O log leva só o id do convite, nunca o
    // token nem o valor cifrado.
    public Optional<String> decifrar(String tokenCifrado, Long conviteId) {
        if (tokenCifrado == null) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(tokenCifrado);
            if (bytes.length <= TAMANHO_IV_BYTES) {
                throw new IllegalArgumentException("valor curto demais");
            }
            Cipher cipher = Cipher.getInstance(CIFRA);
            cipher.init(Cipher.DECRYPT_MODE, chaveCifra,
                    new GCMParameterSpec(TAMANHO_TAG_BITS, bytes, 0, TAMANHO_IV_BYTES));
            byte[] claro = cipher.doFinal(bytes, TAMANHO_IV_BYTES, bytes.length - TAMANHO_IV_BYTES);
            return Optional.of(new String(claro, StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Não foi possível decifrar o token do convite {}: {}", conviteId, e.getClass().getSimpleName());
            return Optional.empty();
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
            convite.setTokenCifrado(null);
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
