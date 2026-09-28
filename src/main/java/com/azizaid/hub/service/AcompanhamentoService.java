package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.request.FiltroAcompanhamentos;
import com.azizaid.hub.dto.request.FiltroAcompanhamentos.CampoData;
import com.azizaid.hub.dto.response.AcompanhamentoResumoDTO;
import com.azizaid.hub.dto.response.PaginaDTO;
import com.azizaid.hub.dto.response.TipoAcompanhamentoResponseDTO;
import com.azizaid.hub.model.TipoAcompanhamento;
import com.azizaid.hub.model.enums.StatusFichaConverter;
import com.azizaid.hub.repository.FichaRepository;
import com.azizaid.hub.repository.FichaRepository.AcompanhamentoLinha;
import com.azizaid.hub.util.CpfUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AcompanhamentoService {

    private static final StatusFichaConverter STATUS_CONVERTER = new StatusFichaConverter();

    private final FichaRepository fichaRepository;

    public AcompanhamentoService(FichaRepository fichaRepository) {
        this.fichaRepository = fichaRepository;
    }

    // No máximo 3 consultas por página: as linhas, o COUNT e os tipos das fichas da página. Não
    // carrega a entidade Ficha, cujas coleções EAGER trariam o N+1 de volta.
    @Transactional(readOnly = true)
    public PaginaDTO<AcompanhamentoResumoDTO> listar(FiltroAcompanhamentos filtro, int pagina, int tamanho) {
        String termo = (filtro.q() == null || filtro.q().isBlank()) ? null : filtro.q().trim();
        String status = filtro.status() != null ? filtro.status().name() : null;
        Long criadoPorId = filtro.meus()
                ? CurrentUser.id().orElseThrow(() -> new AccessDeniedException("Acesso negado"))
                : null;

        // Colunas TIMESTAMP sem fuso: [de 00:00, dia seguinte a ate 00:00), inclusivo nas duas pontas.
        LocalDateTime inicio = inicioDoDia(filtro.de());
        LocalDateTime fimExclusivo = filtro.ate() != null ? inicioDoDia(filtro.ate().plusDays(1)) : null;
        boolean porCriacao = filtro.campoData() == CampoData.DATA_CRIACAO;
        LocalDateTime inicioAtualizacao = porCriacao ? null : inicio;
        LocalDateTime fimAtualizacao = porCriacao ? null : fimExclusivo;
        LocalDateTime inicioCriacao = porCriacao ? inicio : null;
        LocalDateTime fimCriacao = porCriacao ? fimExclusivo : null;

        List<AcompanhamentoLinha> linhas = fichaRepository.listarAcompanhamentos(
                termo, status, criadoPorId, inicioAtualizacao, fimAtualizacao, inicioCriacao, fimCriacao,
                filtro.tipoId(), filtro.servicoId(), tamanho, (long) pagina * tamanho);
        long total = fichaRepository.contarAcompanhamentos(
                termo, status, criadoPorId, inicioAtualizacao, fimAtualizacao, inicioCriacao, fimCriacao,
                filtro.tipoId(), filtro.servicoId());

        Map<Long, List<TipoAcompanhamentoResponseDTO>> tipos = tiposPorFicha(linhas);
        List<AcompanhamentoResumoDTO> itens = linhas.stream()
                .map(linha -> paraResumo(linha, tipos.getOrDefault(linha.getId(), List.of())))
                .toList();

        return PaginaDTO.de(itens, pagina, tamanho, total);
    }

    private Map<Long, List<TipoAcompanhamentoResponseDTO>> tiposPorFicha(List<AcompanhamentoLinha> linhas) {
        Map<Long, List<TipoAcompanhamentoResponseDTO>> tipos = new HashMap<>();
        // IN () vazio dá erro em alguns casos; página vazia não precisa de consulta.
        if (linhas.isEmpty()) {
            return tipos;
        }
        List<Long> ids = linhas.stream().map(AcompanhamentoLinha::getId).toList();
        for (Object[] linha : fichaRepository.buscarTiposAcompanhamentoDasFichas(ids)) {
            Long fichaId = (Long) linha[0];
            TipoAcompanhamento tipo = (TipoAcompanhamento) linha[1];
            tipos.computeIfAbsent(fichaId, id -> new ArrayList<>())
                    .add(TipoAcompanhamentoResponseDTO.from(tipo));
        }
        return tipos;
    }

    private static AcompanhamentoResumoDTO paraResumo(AcompanhamentoLinha linha,
                                                      List<TipoAcompanhamentoResponseDTO> tipos) {
        var status = STATUS_CONVERTER.convertToEntityAttribute(linha.getStatus());
        return new AcompanhamentoResumoDTO(
                linha.getId(),
                linha.getNumeroCaso(),
                linha.getCodigoFicha(),
                linha.getNome(),
                CpfUtils.mascarar(linha.getCpf()),
                linha.getServicoNome(),
                linha.getServicoId() != null ? String.valueOf(linha.getServicoId()) : null,
                status != null ? status.name() : null,
                linha.getDataAtualizacao(),
                linha.getDataCriacao(),
                linha.getCriadoPorId(),
                tipos
        );
    }

    private static LocalDateTime inicioDoDia(LocalDate data) {
        return data != null ? data.atStartOfDay() : null;
    }
}
