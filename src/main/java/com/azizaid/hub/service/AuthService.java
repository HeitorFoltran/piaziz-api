package com.azizaid.hub.service;

import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.config.RateLimitProperties;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.response.LoginResponseDTO;
import com.azizaid.hub.exception.CredenciaisInvalidasException;
import com.azizaid.hub.exception.MuitasTentativasException;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final String MENSAGEM_GENERICA = "Credenciais inválidas";
    private static final String MENSAGEM_MUITAS_TENTATIVAS = "Muitas requisições. Tente novamente em alguns minutos.";
    private static final String MOTIVO_LIMITE_POR_EMAIL = "limite por email";

    private final ProfissionalRepository profissionalRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final AuthAuditLogService authAuditLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitProperties rateLimitProperties;

    public AuthService(ProfissionalRepository profissionalRepository,
                       AuthAuditLogRepository authAuditLogRepository,
                       AuthAuditLogService authAuditLogService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RateLimitProperties rateLimitProperties) {
        this.profissionalRepository = profissionalRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.authAuditLogService = authAuditLogService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String ipAddress) {
        RateLimitProperties.LoginPorEmail limite = rateLimitProperties.loginPorEmail();
        LocalDateTime desde = LocalDateTime.now().minusMinutes(limite.janelaMinutos());
        long falhas = authAuditLogRepository.contarFalhasRecentes(dto.email(), desde, MOTIVO_LIMITE_POR_EMAIL);
        if (falhas >= limite.maxFalhas()) {
            authAuditLogService.registrar(dto.email(), false, ipAddress, MOTIVO_LIMITE_POR_EMAIL);
            throw new MuitasTentativasException(MENSAGEM_MUITAS_TENTATIVAS);
        }

        Optional<Profissional> encontrado = profissionalRepository.findByEmail(dto.email());

        if (encontrado.isEmpty()) {
            authAuditLogService.registrar(dto.email(), false, ipAddress, "email desconhecido");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        Profissional profissional = encontrado.get();
        if (!passwordEncoder.matches(dto.senha(), profissional.getSenhaHash())) {
            authAuditLogService.registrar(dto.email(), false, ipAddress, "senha incorreta");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        if (!profissional.isAtivo()) {
            authAuditLogService.registrar(dto.email(), false, ipAddress, "conta inativa");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        authAuditLogService.registrar(dto.email(), true, ipAddress, null);

        String token = jwtService.gerarToken(profissional.getId(), profissional.getEmail(), profissional.getRole());
        return new LoginResponseDTO(
                token,
                profissional.getId(),
                profissional.getNome(),
                profissional.getEmail(),
                profissional.getRole().name());
    }
}
