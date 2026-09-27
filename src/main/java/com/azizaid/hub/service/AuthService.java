package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.config.JwtService;
import com.azizaid.hub.config.RateLimitProperties;
import com.azizaid.hub.dto.request.LoginRequestDTO;
import com.azizaid.hub.dto.request.TrocarSenhaRequestDTO;
import com.azizaid.hub.dto.response.LoginResponseDTO;
import com.azizaid.hub.dto.response.UsuarioAtualResponseDTO;
import com.azizaid.hub.exception.CredenciaisInvalidasException;
import com.azizaid.hub.exception.MuitasTentativasException;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.enums.AcaoConta;
import com.azizaid.hub.repository.AuthAuditLogRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.util.ContaUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final String MENSAGEM_GENERICA = "Credenciais inválidas";
    private static final String MENSAGEM_MUITAS_TENTATIVAS = "Muitas requisições. Tente novamente em alguns minutos.";
    private static final String MOTIVO_LIMITE_POR_CONTA = "limite por conta";

    private final ProfissionalRepository profissionalRepository;
    private final AuthAuditLogRepository authAuditLogRepository;
    private final AuthAuditLogService authAuditLogService;
    private final ContaAuditLogService contaAuditLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitProperties rateLimitProperties;
    // Hash de uma senha qualquer: quando o identificador não corresponde a nenhuma conta, o login ainda
    // roda um BCrypt contra ele, para o tempo de resposta não revelar quais usernames existem.
    private final String hashFicticio;

    public AuthService(ProfissionalRepository profissionalRepository,
                       AuthAuditLogRepository authAuditLogRepository,
                       AuthAuditLogService authAuditLogService,
                       ContaAuditLogService contaAuditLogService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RateLimitProperties rateLimitProperties) {
        this.profissionalRepository = profissionalRepository;
        this.authAuditLogRepository = authAuditLogRepository;
        this.authAuditLogService = authAuditLogService;
        this.contaAuditLogService = contaAuditLogService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimitProperties = rateLimitProperties;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-so-para-igualar-o-tempo");
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String ipAddress) {
        String identificador = ContaUtils.normalizar(dto.identificador());
        Optional<Profissional> encontrado = identificador.contains("@")
                ? profissionalRepository.findByEmail(identificador)
                : profissionalRepository.findByUsername(identificador);
        Long profissionalId = encontrado.map(Profissional::getId).orElse(null);

        // Com conta: conta as falhas pela conta, para que alternar email e username não dobre as
        // tentativas. Sem conta: pelo identificador digitado, com o mesmo comportamento (sem revelar
        // se a conta existe).
        RateLimitProperties.LoginPorEmail limite = rateLimitProperties.loginPorEmail();
        LocalDateTime desde = LocalDateTime.now().minusMinutes(limite.janelaMinutos());
        long falhas = profissionalId != null
                ? authAuditLogRepository.contarFalhasRecentesDaConta(profissionalId, desde, MOTIVO_LIMITE_POR_CONTA)
                : authAuditLogRepository.contarFalhasRecentes(identificador, desde, MOTIVO_LIMITE_POR_CONTA);
        if (falhas >= limite.maxFalhas()) {
            authAuditLogService.registrar(identificador, profissionalId, false, ipAddress, MOTIVO_LIMITE_POR_CONTA);
            throw new MuitasTentativasException(MENSAGEM_MUITAS_TENTATIVAS);
        }

        if (encontrado.isEmpty()) {
            passwordEncoder.matches(dto.senha(), hashFicticio);
            authAuditLogService.registrar(identificador, null, false, ipAddress, "identificador desconhecido");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        Profissional profissional = encontrado.get();
        if (!passwordEncoder.matches(dto.senha(), profissional.getSenhaHash())) {
            authAuditLogService.registrar(identificador, profissionalId, false, ipAddress, "senha incorreta");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        if (!profissional.isAtivo()) {
            authAuditLogService.registrar(identificador, profissionalId, false, ipAddress, "conta inativa");
            throw new CredenciaisInvalidasException(MENSAGEM_GENERICA);
        }

        authAuditLogService.registrar(identificador, profissionalId, true, ipAddress, null);
        return montarResposta(profissional);
    }

    @Transactional(readOnly = true)
    public UsuarioAtualResponseDTO usuarioAtual() {
        return UsuarioAtualResponseDTO.from(profissionalAtual());
    }

    // Derruba as outras sessões e devolve um token novo para a atual continuar: o JwtAuthFilter
    // compara o iat com a revogação truncada em segundos, então um token emitido agora é aceito.
    @Transactional
    public LoginResponseDTO trocarSenha(TrocarSenhaRequestDTO dto) {
        Profissional profissional = profissionalAtual();
        if (!passwordEncoder.matches(dto.senhaAtual(), profissional.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual inválida");
        }
        ContaUtils.validarTamanhoSenha(dto.novaSenha());
        if (passwordEncoder.matches(dto.novaSenha(), profissional.getSenhaHash())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual");
        }

        profissional.setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        profissional.setDeveTrocarSenha(false);
        profissional.setSessoesRevogadasEm(Instant.now());
        profissionalRepository.save(profissional);
        contaAuditLogService.registrar(profissional.getId(), profissional.getId(), AcaoConta.TROCAR_PROPRIA_SENHA, null);

        return montarResposta(profissional);
    }

    private Profissional profissionalAtual() {
        // O JwtAuthFilter só autentica quem existe e está ativo; vazio aqui é bug, não entrada inválida.
        return CurrentUser.id()
                .flatMap(profissionalRepository::findById)
                .orElseThrow(() -> new IllegalStateException("Requisição autenticada sem profissional"));
    }

    private LoginResponseDTO montarResposta(Profissional profissional) {
        String token = jwtService.gerarToken(profissional.getId(), profissional.getUsername(), profissional.getRole());
        return new LoginResponseDTO(
                token,
                profissional.getId(),
                profissional.getNome(),
                profissional.getUsername(),
                profissional.getEmail(),
                profissional.getRole().name(),
                profissional.isDeveTrocarSenha());
    }
}
