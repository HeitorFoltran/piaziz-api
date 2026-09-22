package com.azizaid.hub.service;

import com.azizaid.hub.config.ConviteTokenService;
import com.azizaid.hub.config.ConviteTokenService.MotivoTokenInvalido;
import com.azizaid.hub.config.ConviteTokenService.ValidacaoTokenResult;
import com.azizaid.hub.dto.request.FichaPublicaRequestDTO;
import com.azizaid.hub.dto.response.FichaPublicaStatusResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.ConviteFicha;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.ResultadoFichaPublica;
import com.azizaid.hub.model.enums.StatusConvite;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.ConviteFichaRepository;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.azizaid.hub.util.CpfUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FichaPublicaService {

    private static final String MENSAGEM_LINK_INVALIDO = "Link inválido ou expirado";

    private final ConviteTokenService conviteTokenService;
    private final ConviteFichaRepository conviteFichaRepository;
    private final FichaPendenteRepository fichaPendenteRepository;
    private final FichaPublicaAuditLogService auditLogService;

    public FichaPublicaService(ConviteTokenService conviteTokenService,
                                ConviteFichaRepository conviteFichaRepository,
                                FichaPendenteRepository fichaPendenteRepository,
                                FichaPublicaAuditLogService auditLogService) {
        this.conviteTokenService = conviteTokenService;
        this.conviteFichaRepository = conviteFichaRepository;
        this.fichaPendenteRepository = fichaPendenteRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FichaPublicaStatusResponseDTO status(String token) {
        ValidacaoTokenResult resultado = conviteTokenService.validar(token);
        if (resultado.valido()) {
            return new FichaPublicaStatusResponseDTO(true, null);
        }
        return new FichaPublicaStatusResponseDTO(false, mapMotivoPublico(resultado.motivo()));
    }

    @Transactional
    public void submeter(String token, FichaPublicaRequestDTO dto, String ip) {
        if (!CpfUtils.isValido(dto.cpf())) {
            auditLogService.registrar(null, ip, ResultadoFichaPublica.VALIDACAO_FALHOU);
            throw new IllegalArgumentException("CPF inválido");
        }

        ValidacaoTokenResult resultado = conviteTokenService.validar(token);
        if (!resultado.valido()) {
            auditLogService.registrar(null, ip, mapResultadoAudit(resultado.motivo()));
            throw new RecursoNaoEncontradoException(MENSAGEM_LINK_INVALIDO);
        }

        ConviteFicha convite = resultado.convite();
        int atualizados = conviteFichaRepository.marcarComoUsadoSeAtivo(
                convite.getId(), StatusConvite.USADO, StatusConvite.ATIVO, LocalDateTime.now());
        if (atualizados == 0) {
            auditLogService.registrar(convite.getId(), ip, ResultadoFichaPublica.TOKEN_JA_USADO);
            throw new RecursoNaoEncontradoException(MENSAGEM_LINK_INVALIDO);
        }

        FichaPendente pendente = new FichaPendente();
        pendente.setConviteId(convite.getId());
        pendente.setNome(dto.nome());
        pendente.setCpf(dto.cpf());
        pendente.setTelefone(dto.telefone());
        pendente.setIdade(dto.idade());
        pendente.setSituacaoRelatada(dto.situacaoRelatada());
        pendente.setIpSubmissao(ip);
        pendente.setStatus(StatusFichaPendente.PENDENTE);
        fichaPendenteRepository.save(pendente);

        auditLogService.registrar(convite.getId(), ip, ResultadoFichaPublica.SUBMETIDO);
    }

    private String mapMotivoPublico(MotivoTokenInvalido motivo) {
        return switch (motivo) {
            case INVALIDO -> "invalido";
            case EXPIRADO -> "expirado";
            case USADO -> "usado";
        };
    }

    private ResultadoFichaPublica mapResultadoAudit(MotivoTokenInvalido motivo) {
        return switch (motivo) {
            case INVALIDO -> ResultadoFichaPublica.TOKEN_INVALIDO;
            case EXPIRADO -> ResultadoFichaPublica.TOKEN_EXPIRADO;
            case USADO -> ResultadoFichaPublica.TOKEN_JA_USADO;
        };
    }
}
