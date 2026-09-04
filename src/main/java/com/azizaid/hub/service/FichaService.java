package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.TipoAcompanhamentoRepository;
import com.azizaid.hub.util.CpfUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FichaService {

    private final FichaRepository fichaRepository;
    private final EntityAuditService entityAuditService;
    private final TipoAcompanhamentoRepository tipoAcompanhamentoRepository;

    public FichaService(FichaRepository fichaRepository, EntityAuditService entityAuditService,
                         TipoAcompanhamentoRepository tipoAcompanhamentoRepository) {
        this.fichaRepository = fichaRepository;
        this.entityAuditService = entityAuditService;
        this.tipoAcompanhamentoRepository = tipoAcompanhamentoRepository;
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

        Ficha salvo = fichaRepository.save(ficha);
        return FichaResponseDTO.from(salvo);
    }

    @Transactional
    public FichaResponseDTO atualizar(Long id, FichaRequestDTO dto) {
        Ficha ficha = buscarEntidade(id);
        if (!CpfUtils.isValido(dto.cpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }
        registrarEdicaoCrossUserSeAplicavel(ficha);
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
        return FichaResponseDTO.from(fichaRepository.save(ficha));
    }

    @Transactional
    public FichaResponseDTO atualizarStatus(Long id, StatusFicha novoStatus) {
        Ficha ficha = buscarEntidade(id);
        registrarEdicaoCrossUserSeAplicavel(ficha);
        ficha.setStatus(novoStatus);
        return FichaResponseDTO.from(fichaRepository.save(ficha));
    }

    @Transactional
    public FichaResponseDTO atribuirTiposAcompanhamento(Long fichaId, List<Long> tipoIds) {
        Ficha ficha = buscarEntidade(fichaId);
        registrarEdicaoCrossUserSeAplicavel(ficha);

        Set<TipoAcompanhamento> tipos = new HashSet<>(tipoAcompanhamentoRepository.findAllById(tipoIds));
        if (tipos.size() != new HashSet<>(tipoIds).size()) {
            throw new IllegalArgumentException("Um ou mais tipos de acompanhamento informados não existem");
        }

        ficha.setTiposAcompanhamento(tipos);
        return FichaResponseDTO.from(fichaRepository.save(ficha));
    }

    private void registrarEdicaoCrossUserSeAplicavel(Ficha ficha) {
        Long donoId = ficha.getCriadoPorId() != null ? ficha.getCriadoPorId() : ficha.getUltimoEditorId();
        entityAuditService.registrarSeCrossUser("Ficha", ficha.getId(), donoId);
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
