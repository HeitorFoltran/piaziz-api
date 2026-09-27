package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.request.ProfissionalEdicaoRequestDTO;
import com.azizaid.hub.dto.request.ProfissionalRequestDTO;
import com.azizaid.hub.dto.request.ResetarSenhaRequestDTO;
import com.azizaid.hub.dto.response.ProfissionalResponseDTO;
import com.azizaid.hub.exception.OperacaoNaoPermitidaException;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.model.enums.AcaoConta;
import com.azizaid.hub.model.enums.PapelProfissional;
import com.azizaid.hub.repository.ProfissionalRepository;
import com.azizaid.hub.repository.ServicoRepository;
import com.azizaid.hub.util.ContaUtils;
import com.azizaid.hub.util.CpfUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

// Todas as rotas que chegam aqui já passaram por @permissoes.podeGerenciarProfissionais(). As regras
// abaixo são as que dependem de quem é o autor e de qual conta é o alvo.
@Service
public class ProfissionalService {

    private static final Pattern FORMATO_USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContaAuditLogService contaAuditLogService;

    public ProfissionalService(ProfissionalRepository profissionalRepository, ServicoRepository servicoRepository,
                               PasswordEncoder passwordEncoder, ContaAuditLogService contaAuditLogService) {
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.contaAuditLogService = contaAuditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalResponseDTO> listar(String termo, Long servicoId) {
        String t = (termo == null || termo.isBlank()) ? null : termo.trim();
        return profissionalRepository.buscar(t, servicoId).stream()
                .map(ProfissionalResponseDTO::resumo)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalResponseDTO buscarPorId(Long id) {
        return ProfissionalResponseDTO.from(buscarEntidade(id));
    }

    @Transactional
    public ProfissionalResponseDTO criar(ProfissionalRequestDTO dto) {
        Profissional autor = autorAtual();

        String username = validarUsernameNovo(dto.username());
        String email = validarEmailNovo(dto.email());
        String cpf = validarCpfNovo(dto.cpf());
        PapelProfissional role = resolverRole(dto.role());
        if (role == PapelProfissional.DEV) {
            throw new IllegalArgumentException("role DEV não pode ser atribuída por esta rota");
        }
        boolean podeGerenciar = Boolean.TRUE.equals(dto.podeGerenciarProfissionais());
        if (podeGerenciar && (autor.getRole() != PapelProfissional.DEV || role != PapelProfissional.PADRAO)) {
            throw new OperacaoNaoPermitidaException(
                    "Só o DEV concede a permissão de gerenciar profissionais, e só a contas PADRAO");
        }
        ContaUtils.validarTamanhoSenha(dto.senhaProvisoria());

        Profissional profissional = new Profissional();
        profissional.setNome(dto.nome().trim());
        profissional.setCpf(cpf);
        profissional.setCarteiraProfissional(dto.carteiraProfissional());
        profissional.setServico(resolverServico(dto.servicoId()));
        profissional.setUsername(username);
        profissional.setEmail(email);
        profissional.setSenhaHash(passwordEncoder.encode(dto.senhaProvisoria()));
        profissional.setRole(role);
        profissional.setPodeGerenciarProfissionais(podeGerenciar);
        profissional.setDeveTrocarSenha(true);

        Profissional salvo = salvarComUnicidade(profissional);
        contaAuditLogService.registrar(salvo.getId(), autor.getId(), AcaoConta.CRIAR,
                "username: " + salvo.getUsername() + "; role: " + salvo.getRole().name()
                        + (podeGerenciar ? "; podeGerenciarProfissionais" : ""));
        return ProfissionalResponseDTO.from(salvo);
    }

    @Transactional
    public ProfissionalResponseDTO editar(Long id, ProfissionalEdicaoRequestDTO dto) {
        Profissional autor = autorAtual();
        Profissional alvo = buscarEntidade(id);
        boolean autorDev = autor.getRole() == PapelProfissional.DEV;
        boolean propriaConta = autor.getId().equals(alvo.getId());

        exigirPodeMexerEm(autorDev, propriaConta, alvo);

        PapelProfissional role = resolverRole(dto.role());
        boolean roleMudou = role != alvo.getRole();
        boolean flagMudou = dto.podeGerenciarProfissionais() != alvo.isPodeGerenciarProfissionais();
        boolean ativoMudou = dto.ativo() != alvo.isAtivo();

        if (roleMudou && (role == PapelProfissional.DEV || alvo.getRole() == PapelProfissional.DEV)) {
            throw new OperacaoNaoPermitidaException("A role DEV só é atribuída ou retirada direto no banco");
        }
        if (flagMudou && !autorDev) {
            throw new OperacaoNaoPermitidaException("Só o DEV concede ou retira a permissão de gerenciar profissionais");
        }
        if (dto.podeGerenciarProfissionais() && role != PapelProfissional.PADRAO) {
            throw new OperacaoNaoPermitidaException("A permissão de gerenciar profissionais só vale para contas PADRAO");
        }
        if (propriaConta && (roleMudou || flagMudou || ativoMudou)) {
            throw new OperacaoNaoPermitidaException("Não é possível alterar a própria role, permissão ou status");
        }

        List<String> alterados = new ArrayList<>();

        String nome = dto.nome().trim();
        if (!nome.equals(alvo.getNome())) {
            alvo.setNome(nome);
            alterados.add("nome");
        }

        String username = ContaUtils.normalizar(dto.username());
        if (!Objects.equals(username, alvo.getUsername())) {
            validarUsernameNovo(username);
            alterados.add("username (" + alvo.getUsername() + " -> " + username + ")");
            alvo.setUsername(username);
        }

        String email = ContaUtils.normalizar(dto.email());
        if (!Objects.equals(email, alvo.getEmail())) {
            validarEmailNovo(email);
            alvo.setEmail(email);
            alterados.add("email");
        }

        // Contas antigas com CPF inválido continuam salváveis desde que o CPF não mude.
        if (!Objects.equals(CpfUtils.digitos(dto.cpf()), CpfUtils.digitos(alvo.getCpf()))) {
            alvo.setCpf(validarCpfNovo(dto.cpf()));
            alterados.add("cpf");
        }

        if (!Objects.equals(dto.carteiraProfissional(), alvo.getCarteiraProfissional())) {
            alvo.setCarteiraProfissional(dto.carteiraProfissional());
            alterados.add("carteiraProfissional");
        }

        Long servicoAtualId = alvo.getServico() != null ? alvo.getServico().getId() : null;
        if (!Objects.equals(dto.servicoId(), servicoAtualId)) {
            alvo.setServico(resolverServico(dto.servicoId()));
            alterados.add("servico");
        }

        if (roleMudou) {
            alvo.setRole(role);
            alterados.add("role");
        }
        if (flagMudou) {
            alvo.setPodeGerenciarProfissionais(dto.podeGerenciarProfissionais());
            alterados.add("podeGerenciarProfissionais");
        }
        if (ativoMudou) {
            alvo.setAtivo(dto.ativo());
            if (!dto.ativo()) {
                alvo.setSessoesRevogadasEm(Instant.now());
            }
            alterados.add("ativo");
        }

        Profissional salvo = salvarComUnicidade(alvo);
        if (!alterados.isEmpty()) {
            contaAuditLogService.registrar(salvo.getId(), autor.getId(), AcaoConta.EDITAR,
                    "campos: " + String.join(", ", alterados));
        }
        return ProfissionalResponseDTO.from(salvo);
    }

    @Transactional
    public void resetarSenha(Long id, ResetarSenhaRequestDTO dto) {
        Profissional autor = autorAtual();
        Profissional alvo = buscarEntidade(id);

        exigirPodeMexerEm(autor.getRole() == PapelProfissional.DEV, autor.getId().equals(alvo.getId()), alvo);
        if (autor.getId().equals(alvo.getId())) {
            throw new OperacaoNaoPermitidaException("Para trocar a própria senha, use PUT /api/auth/senha");
        }
        ContaUtils.validarTamanhoSenha(dto.senhaProvisoria());

        alvo.setSenhaHash(passwordEncoder.encode(dto.senhaProvisoria()));
        alvo.setDeveTrocarSenha(true);
        alvo.setSessoesRevogadasEm(Instant.now());
        profissionalRepository.save(alvo);
        contaAuditLogService.registrar(alvo.getId(), autor.getId(), AcaoConta.RESETAR_SENHA, null);
    }

    // Conta DEV e conta de gerenciador: só o DEV mexe. Senão um gerenciador poderia trancar os outros
    // para fora, ou resetar a senha de um colega e entrar como ele antes da troca. A própria conta fica
    // fora desta regra: editar tem as travas dela (role, ativo, flag) e resetar a própria senha é recusado.
    private static void exigirPodeMexerEm(boolean autorDev, boolean propriaConta, Profissional alvo) {
        if (autorDev || propriaConta) {
            return;
        }
        if (alvo.getRole() == PapelProfissional.DEV || alvo.isPodeGerenciarProfissionais()) {
            throw new OperacaoNaoPermitidaException("Só o DEV pode alterar uma conta DEV ou de gerenciador");
        }
    }

    // As checagens de username/email não pegam duas requisições simultâneas com o mesmo valor: a segunda
    // bate na constraint UNIQUE do banco. O saveAndFlush faz o erro aparecer aqui, e ele vira o mesmo 400
    // da checagem normal, em vez de um 500.
    private Profissional salvarComUnicidade(Profissional profissional) {
        try {
            return profissionalRepository.saveAndFlush(profissional);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("username ou email já em uso");
        }
    }

    private Profissional autorAtual() {
        // O @PreAuthorize do controller já garantiu que há um gerenciador autenticado.
        return CurrentUser.id()
                .flatMap(profissionalRepository::findById)
                .orElseThrow(() -> new IllegalStateException("Requisição autenticada sem profissional"));
    }

    private Profissional buscarEntidade(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Profissional", id));
    }

    private Servico resolverServico(Long servicoId) {
        if (servicoId == null) {
            return null;
        }
        return servicoRepository.findById(servicoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", servicoId));
    }

    private String validarUsernameNovo(String bruto) {
        String username = ContaUtils.normalizar(bruto);
        if (username == null || !FORMATO_USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "username deve ter de 3 a 30 caracteres: letras minúsculas, números, ponto, hífen ou sublinhado");
        }
        if (profissionalRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username já em uso");
        }
        return username;
    }

    private String validarEmailNovo(String bruto) {
        String email = ContaUtils.normalizar(bruto);
        if (email != null && profissionalRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email já em uso");
        }
        return email;
    }

    // Grava no formato 000.000.000-00. A checagem de duplicidade cobre também registros antigos
    // gravados só com os dígitos.
    private String validarCpfNovo(String bruto) {
        if (!CpfUtils.isValido(bruto)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        String formatado = CpfUtils.formatar(bruto);
        if (profissionalRepository.existsByCpfIn(List.of(formatado, CpfUtils.digitos(bruto)))) {
            throw new IllegalArgumentException("Já existe um profissional com este CPF");
        }
        return formatado;
    }

    private static PapelProfissional resolverRole(String role) {
        try {
            return PapelProfissional.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("role inválida: " + role);
        }
    }
}
