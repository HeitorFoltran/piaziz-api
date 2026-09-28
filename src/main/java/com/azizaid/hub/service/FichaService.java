package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.model.enums.NecessidadeImediata;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import com.azizaid.hub.util.CpfUtils;
import com.azizaid.hub.util.RetratoAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FichaService {

    private final FichaRepository fichaRepository;
    private final EntityAuditService entityAuditService;
    private final TipoAcompanhamentoRepository tipoAcompanhamentoRepository;
    private final LeituraAuditService leituraAuditService;

    public FichaService(FichaRepository fichaRepository, EntityAuditService entityAuditService,
                         TipoAcompanhamentoRepository tipoAcompanhamentoRepository,
                         LeituraAuditService leituraAuditService) {
        this.fichaRepository = fichaRepository;
        this.entityAuditService = entityAuditService;
        this.tipoAcompanhamentoRepository = tipoAcompanhamentoRepository;
        this.leituraAuditService = leituraAuditService;
    }

    @Transactional(readOnly = true)
    public List<FichaResponseDTO> listar(String termo) {
        return fichaRepository.buscar(normalizar(termo)).stream()
                .map(FichaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FichaResponseDTO detalhar(Long id) {
        Ficha ficha = buscarEntidade(id);
        return FichaResponseDTO.from(ficha);
    }

    // Só para GET /api/fichas/{id}: registra quem abriu o caso. Não usar em fluxos internos, que
    // registrariam leituras que ninguém fez; para esses, detalhar() ou buscarEntidade().
    @Transactional(readOnly = true)
    public FichaResponseDTO abrirParaLeitura(Long id) {
        FichaResponseDTO ficha = FichaResponseDTO.from(buscarEntidade(id));
        CurrentUser.id().ifPresent(profissionalId -> leituraAuditService.registrar(id, profissionalId));
        return ficha;
    }

    @Transactional(readOnly = true)
    public Ficha buscarEntidade(Long id) {
        return fichaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Ficha", id));
    }

    @Transactional
    public FichaResponseDTO criar(FichaRequestDTO dto) {
        if (!CpfUtils.isValido(dto.cpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (fichaRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException("Já existe uma ficha cadastrada com este CPF");
        }

        int sequencial = fichaRepository.buscarMaiorSequencialCodigo() + 1;

        Ficha ficha = new Ficha();
        ficha.setCodigoFicha(gerarCodigoFicha(sequencial));
        ficha.setNumeroCaso(resolverNumeroCaso(dto.numeroCaso(), sequencial));
        ficha.setNome(dto.nome());
        ficha.setCpf(dto.cpf());
        ficha.setIdade(dto.idade());
        ficha.setTelefone(dto.telefone());
        ficha.setEstadoCivil(dto.estadoCivil());
        ficha.setPessoasDependentes(dto.pessoasDependentes());
        ficha.setIdadeFilhos(dto.idadeFilhos());
        ficha.setNivelSeguranca(dto.nivelSeguranca());
        ficha.setTipoMoradia(dto.tipoMoradia());
        ficha.setTipoMoradiaOutraDescricao(dto.tipoMoradiaOutraDescricao());
        ficha.setQtdMoradores(dto.qtdMoradores());
        ficha.setQtdFilhos(dto.qtdFilhos());
        ficha.setOndeMoramFilhos(dto.ondeMoramFilhos());
        ficha.setSupervisaoFilhos(dto.supervisaoFilhos());
        if (dto.vagasNecessarias() != null) ficha.setVagasNecessarias(dto.vagasNecessarias());
        if (dto.necessidadesImediatas() != null) ficha.setNecessidadesImediatas(dto.necessidadesImediatas());
        ficha.setNecessidadeOutraDescricao(dto.necessidadeOutraDescricao());
        ficha.setStatus(dto.status() != null ? dto.status() : StatusFicha.ATIVO);
        marcarOutroSeTemDescricao(ficha);

        Ficha salvo = fichaRepository.save(ficha);
        return FichaResponseDTO.from(salvo);
    }

    @Transactional
    public FichaResponseDTO atualizar(Long id, FichaRequestDTO dto) {
        Ficha ficha = buscarEntidade(id);
        if (!CpfUtils.isValido(dto.cpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }
        List<Object> antes = retratoDados(ficha);
        StatusFicha statusAntes = ficha.getStatus();

        ficha.setNome(dto.nome());
        ficha.setCpf(dto.cpf());
        ficha.setNumeroCaso(dto.numeroCaso() != null && !dto.numeroCaso().isBlank()
                ? dto.numeroCaso() : ficha.getNumeroCaso());
        ficha.setIdade(dto.idade());
        ficha.setTelefone(dto.telefone());
        ficha.setEstadoCivil(dto.estadoCivil());
        ficha.setPessoasDependentes(dto.pessoasDependentes());
        ficha.setIdadeFilhos(dto.idadeFilhos());
        ficha.setNivelSeguranca(dto.nivelSeguranca());
        ficha.setTipoMoradia(dto.tipoMoradia());
        ficha.setTipoMoradiaOutraDescricao(dto.tipoMoradiaOutraDescricao());
        ficha.setQtdMoradores(dto.qtdMoradores());
        ficha.setQtdFilhos(dto.qtdFilhos());
        ficha.setOndeMoramFilhos(dto.ondeMoramFilhos());
        ficha.setSupervisaoFilhos(dto.supervisaoFilhos());
        if (dto.vagasNecessarias() != null) ficha.setVagasNecessarias(dto.vagasNecessarias());
        if (dto.necessidadesImediatas() != null) ficha.setNecessidadesImediatas(dto.necessidadesImediatas());
        ficha.setNecessidadeOutraDescricao(dto.necessidadeOutraDescricao());
        if (dto.status() != null) ficha.setStatus(dto.status());
        marcarOutroSeTemDescricao(ficha);

        if (RetratoAuditoria.mudou(antes, retratoDados(ficha))) {
            registrar(ficha, "Ficha", AcaoAlteracao.EDITOU, null);
        }
        registrarMudancaDeStatus(ficha, statusAntes);
        return FichaResponseDTO.from(fichaRepository.save(ficha));
    }

    @Transactional
    public FichaResponseDTO atualizarStatus(Long id, StatusFicha novoStatus) {
        Ficha ficha = buscarEntidade(id);
        StatusFicha statusAntes = ficha.getStatus();
        ficha.setStatus(novoStatus);
        registrarMudancaDeStatus(ficha, statusAntes);
        return FichaResponseDTO.from(fichaRepository.save(ficha));
    }

    @Transactional
    public FichaResponseDTO atribuirTiposAcompanhamento(Long fichaId, List<Long> tipoIds) {
        Ficha ficha = buscarEntidade(fichaId);

        Set<TipoAcompanhamento> tipos = new HashSet<>(tipoAcompanhamentoRepository.findAllById(tipoIds));
        if (tipos.size() != new HashSet<>(tipoIds).size()) {
            throw new IllegalArgumentException("Um ou mais tipos de acompanhamento informados não existem");
        }

        Set<Long> idsAntes = ficha.getTiposAcompanhamento().stream()
                .map(TipoAcompanhamento::getId)
                .collect(Collectors.toSet());
        ficha.setTiposAcompanhamento(tipos);
        if (!idsAntes.equals(new HashSet<>(tipoIds))) {
            registrar(ficha, "TiposAcompanhamento", AcaoAlteracao.ALTEROU_TIPOS, null);
        }
        return FichaResponseDTO.from(fichaRepository.save(ficha));
    }

    // "Outro, qual?" preenchido implica OUTRO marcado em "O que eu preciso agora". O contrário não:
    // OUTRO sem descrição é permitido. Garantido aqui porque o link público (aprovado via criar) e
    // clientes antigos mandam só a descrição. Novo set: o que veio do DTO pode ser imutável.
    private void marcarOutroSeTemDescricao(Ficha ficha) {
        String descricao = ficha.getNecessidadeOutraDescricao();
        if (descricao == null || descricao.isBlank()) {
            return;
        }
        Set<NecessidadeImediata> necessidades = ficha.getNecessidadesImediatas() != null
                ? new HashSet<>(ficha.getNecessidadesImediatas()) : new HashSet<>();
        if (necessidades.add(NecessidadeImediata.OUTRO)) {
            ficha.setNecessidadesImediatas(necessidades);
        }
    }

    // Campos gravados pelo PUT da ficha, exceto status e tipos de acompanhamento, que têm linha
    // própria no histórico. Datas de atualização e editor ficam fora: mudam no flush.
    private List<Object> retratoDados(Ficha f) {
        return RetratoAuditoria.de(f.getNome(), f.getCpf(), f.getNumeroCaso(), f.getIdade(), f.getTelefone(),
                f.getEstadoCivil(), f.getPessoasDependentes(), f.getIdadeFilhos(), f.getNivelSeguranca(),
                f.getTipoMoradia(), f.getTipoMoradiaOutraDescricao(), f.getQtdMoradores(), f.getQtdFilhos(),
                f.getOndeMoramFilhos(), f.getSupervisaoFilhos(), f.getVagasNecessarias(),
                f.getNecessidadesImediatas(), f.getNecessidadeOutraDescricao());
    }

    private void registrarMudancaDeStatus(Ficha ficha, StatusFicha statusAntes) {
        if (ficha.getStatus() == statusAntes) {
            return;
        }
        registrar(ficha, "StatusFicha", AcaoAlteracao.MUDOU_STATUS,
                rotulo(statusAntes) + " -> " + rotulo(ficha.getStatus()));
    }

    private void registrar(Ficha ficha, String tipoEntidade, AcaoAlteracao acao, String detalhe) {
        Long donoId = ficha.getCriadoPorId() != null ? ficha.getCriadoPorId() : ficha.getUltimoEditorId();
        entityAuditService.registrar(tipoEntidade, ficha.getId(), donoId, ficha.getId(), acao, detalhe);
    }

    private static String rotulo(StatusFicha status) {
        return status != null ? status.getLabel() : "-";
    }

    private String gerarCodigoFicha(int sequencial) {
        return String.format("F-%04d", sequencial);
    }

    private String resolverNumeroCaso(String informado, int sequencial) {
        if (informado != null && !informado.isBlank()) {
            return informado.trim();
        }
        return String.format("%d/%06d", LocalDate.now().getYear(), sequencial);
    }

    private String normalizar(String termo) {
        return (termo == null || termo.isBlank()) ? null : termo.trim();
    }
}
