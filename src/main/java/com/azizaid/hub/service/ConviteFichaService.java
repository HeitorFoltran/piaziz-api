package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConviteFichaService {

    private final ConviteFichaRepository conviteFichaRepository;
    private final ConviteTokenService conviteTokenService;
    private final String frontendBaseUrl;

    public ConviteFichaService(ConviteFichaRepository conviteFichaRepository,
                                ConviteTokenService conviteTokenService,
                                @Value("${frontend.base-url}") String frontendBaseUrl) {
        this.conviteFichaRepository = conviteFichaRepository;
        this.conviteTokenService = conviteTokenService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public ConviteFichaResponseDTO criar() {
        Long criadoPorId = usuarioAtual();
        String tokenCru = conviteTokenService.gerarTokenCru();

        ConviteFicha convite = ConviteFicha.builder()
                .tokenHash(conviteTokenService.hash(tokenCru))
                .criadoPorId(criadoPorId)
                .dataExpiracao(conviteTokenService.calcularExpiracao())
                .status(StatusConvite.ATIVO)
                .build();

        ConviteFicha salvo = conviteFichaRepository.save(convite);
        String link = frontendBaseUrl + "/ficha-publica/" + tokenCru;
        return ConviteFichaResponseDTO.comLink(salvo, link);
    }

    @Transactional(readOnly = true)
    public List<ConviteFichaResponseDTO> listarMeusConvites() {
        return conviteFichaRepository.findByCriadoPorIdOrderByDataCriacaoDesc(usuarioAtual()).stream()
                .map(ConviteFichaResponseDTO::semLink)
                .toList();
    }

    @Transactional
    public ConviteFichaResponseDTO cancelar(Long id) {
        ConviteFicha convite = conviteFichaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Convite", id));

        Long usuarioId = usuarioAtual();
        if (!usuarioId.equals(convite.getCriadoPorId()) && !CurrentUser.hasRole("DEV")) {
            throw new AccessDeniedException("Você não tem permissão para cancelar este convite");
        }
        if (convite.getStatus() != StatusConvite.ATIVO) {
            throw new IllegalArgumentException("Só é possível cancelar um convite ativo");
        }

        convite.setStatus(StatusConvite.CANCELADO);
        return ConviteFichaResponseDTO.semLink(conviteFichaRepository.save(convite));
    }

    private Long usuarioAtual() {
        return CurrentUser.id()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no contexto"));
    }
}
