package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.response.ConviteFichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConviteFichaService {

    private final ConviteFichaRepository conviteFichaRepository;
    private final ConviteTokenService conviteTokenService;
    private final ProfissionalRepository profissionalRepository;
    private final String frontendBaseUrl;

    public ConviteFichaService(ConviteFichaRepository conviteFichaRepository,
                                ConviteTokenService conviteTokenService,
                                ProfissionalRepository profissionalRepository,
                                @Value("${frontend.base-url}") String frontendBaseUrl) {
        this.conviteFichaRepository = conviteFichaRepository;
        this.conviteTokenService = conviteTokenService;
        this.profissionalRepository = profissionalRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public ConviteFichaResponseDTO criar() {
        Long criadoPorId = usuarioAtual();
        String tokenCru = conviteTokenService.gerarTokenCru();

        ConviteFicha convite = ConviteFicha.builder()
                .tokenHash(conviteTokenService.hash(tokenCru))
                .tokenCifrado(conviteTokenService.cifrar(tokenCru))
                .criadoPorId(criadoPorId)
                .dataExpiracao(conviteTokenService.calcularExpiracao())
                .status(StatusConvite.ATIVO)
                .build();

        ConviteFicha salvo = conviteFichaRepository.save(convite);
        return ConviteFichaResponseDTO.comLink(salvo, montarLink(tokenCru), nomeDe(criadoPorId));
    }

    // Lista da equipe inteira: ESTAGIARIO, PADRAO e DEV veem os links de todo mundo. Cancelar
    // continua restrito a quem gerou ou DEV.
    @Transactional(readOnly = true)
    public List<ConviteFichaResponseDTO> listar() {
        List<ConviteFicha> convites = conviteFichaRepository.findAllByOrderByDataCriacaoDesc();

        List<Long> ids = convites.stream().map(ConviteFicha::getCriadoPorId).distinct().toList();
        Map<Long, String> nomesPorId = profissionalRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Profissional::getId, Profissional::getNome));

        LocalDateTime agora = LocalDateTime.now();
        return convites.stream()
                .map(c -> ConviteFichaResponseDTO.comLink(c, linkSeUtilizavel(c, agora), nomesPorId.get(c.getCriadoPorId())))
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
        convite.setTokenCifrado(null);
        return ConviteFichaResponseDTO.semLink(conviteFichaRepository.save(convite), nomeDe(convite.getCriadoPorId()));
    }

    // Usado, cancelado, vencido (mesmo que ainda ATIVO no banco) ou sem token cifrado: sem link.
    private String linkSeUtilizavel(ConviteFicha c, LocalDateTime agora) {
        if (c.getStatus() != StatusConvite.ATIVO || !c.getDataExpiracao().isAfter(agora)) {
            return null;
        }
        return conviteTokenService.decifrar(c.getTokenCifrado(), c.getId())
                .map(this::montarLink)
                .orElse(null);
    }

    private String montarLink(String tokenCru) {
        return frontendBaseUrl + "/ficha-publica/" + tokenCru;
    }

    private String nomeDe(Long profissionalId) {
        return profissionalRepository.findById(profissionalId).map(Profissional::getNome).orElse(null);
    }

    private Long usuarioAtual() {
        return CurrentUser.id()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no contexto"));
    }
}
