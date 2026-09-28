package com.azizaid.hub.config;

import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConviteTokenServiceTest {

    // 32 bytes em Base64, só para teste.
    private static final String CHAVE = "q2Wm0l1c3XxjJb0cQyC6k1l0p6c9b2K8m3n4v5w6x7Y=";

    @Mock
    ConviteFichaRepository conviteFichaRepository;

    ConviteTokenService conviteTokenService;

    @BeforeEach
    void setUp() {
        conviteTokenService = new ConviteTokenService(conviteFichaRepository, CHAVE);
    }

    @Test
    void cifrarEDecifrar_devolveOTokenOriginal() {
        String token = conviteTokenService.gerarTokenCru();

        String cifrado = conviteTokenService.cifrar(token);

        assertNotEquals(token, cifrado);
        assertEquals(Optional.of(token), conviteTokenService.decifrar(cifrado, 1L));
    }

    @Test
    void cifrar_duasVezesOMesmoToken_geraValoresDiferentes() {
        assertNotEquals(conviteTokenService.cifrar("mesmo-token"), conviteTokenService.cifrar("mesmo-token"));
    }

    @Test
    void decifrar_valorAdulterado_devolveVazioSemLancar() {
        byte[] bytes = Base64.getDecoder().decode(conviteTokenService.cifrar("token"));
        bytes[bytes.length - 1] ^= 0x01;

        assertEquals(Optional.empty(), conviteTokenService.decifrar(Base64.getEncoder().encodeToString(bytes), 1L));
    }

    @Test
    void decifrar_comOutraChave_devolveVazio() {
        String cifrado = conviteTokenService.cifrar("token");
        ConviteTokenService comOutraChave = new ConviteTokenService(conviteFichaRepository,
                Base64.getEncoder().encodeToString(new byte[32]));

        assertEquals(Optional.empty(), comOutraChave.decifrar(cifrado, 1L));
    }

    @Test
    void decifrar_nuloOuLixo_devolveVazio() {
        assertEquals(Optional.empty(), conviteTokenService.decifrar(null, 1L));
        assertEquals(Optional.empty(), conviteTokenService.decifrar("não é base64", 1L));
        assertEquals(Optional.empty(), conviteTokenService.decifrar("AAAA", 1L));
    }

    @Test
    void construtor_chaveComTamanhoErrado_lancaSemExporAChave() {
        String chaveCurta = Base64.getEncoder().encodeToString("dezesseis-bytes!".getBytes());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new ConviteTokenService(conviteFichaRepository, chaveCurta));

        assertEquals("CONVITE_TOKEN_KEY precisa ter 32 bytes em Base64", e.getMessage());
        assertFalse(e.getMessage().contains(chaveCurta));
        assertNull(e.getCause());
    }

    @Test
    void construtor_chaveQueNaoEBase64_lancaSemExporAChave() {
        String chaveInvalida = "isto-nao-e-base64-valido!!";

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new ConviteTokenService(conviteFichaRepository, chaveInvalida));

        assertFalse(e.getMessage().contains(chaveInvalida));
        assertNull(e.getCause());
    }

    @Test
    void validar_conviteAtivoVencido_marcaExpiradoEApagaTokenCifrado() {
        ConviteFicha convite = ConviteFicha.builder().id(1L).criadoPorId(1L).status(StatusConvite.ATIVO)
                .dataExpiracao(LocalDateTime.now().minusMinutes(1)).tokenCifrado("cifrado").build();
        when(conviteFichaRepository.findByTokenHash(any())).thenReturn(Optional.of(convite));

        ConviteTokenService.ValidacaoTokenResult resultado = conviteTokenService.validar("token");

        assertFalse(resultado.valido());
        assertEquals(StatusConvite.EXPIRADO, convite.getStatus());
        assertNull(convite.getTokenCifrado());
        verify(conviteFichaRepository).save(convite);
    }
}
