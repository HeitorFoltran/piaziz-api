package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.dto.response.FichaPendenteResponseDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.FichaPendenteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FichaPendenteService {

    private final FichaPendenteRepository fichaPendenteRepository;
    private final FichaService fichaService;
    private final AvaliacaoSocioeconomicaService avaliacaoSocioeconomicaService;
    private final HistoricoAtendimentoService historicoAtendimentoService;
    private final ObjectMapper objectMapper;

    public FichaPendenteService(FichaPendenteRepository fichaPendenteRepository,
                                 FichaService fichaService,
                                 AvaliacaoSocioeconomicaService avaliacaoSocioeconomicaService,
                                 HistoricoAtendimentoService historicoAtendimentoService,
                                 ObjectMapper objectMapper) {
        this.fichaPendenteRepository = fichaPendenteRepository;
        this.fichaService = fichaService;
        this.avaliacaoSocioeconomicaService = avaliacaoSocioeconomicaService;
        this.historicoAtendimentoService = historicoAtendimentoService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FichaPendenteResponseDTO> listar(StatusFichaPendente status) {
        StatusFichaPendente filtro = status != null ? status : StatusFichaPendente.PENDENTE;
        return fichaPendenteRepository.findByStatusOrderByDataSubmissaoDesc(filtro).stream()
                .map(f -> FichaPendenteResponseDTO.from(f, objectMapper))
                .toList();
    }

    @Transactional
    public FichaPendenteResponseDTO aprovar(Long id) {
        FichaPendente pendente = buscarEntidade(id);
        exigirPendente(pendente);

        FichaRequestDTO fichaDto = desserializar(pendente.getDadosFichaJson(), FichaRequestDTO.class);
        FichaResponseDTO fichaCriada = fichaService.criar(fichaDto);

        if (pendente.getDadosAvaliacaoJson() != null) {
            AvaliacaoSocioeconomicaRequestDTO avaliacaoDto =
                    desserializar(pendente.getDadosAvaliacaoJson(), AvaliacaoSocioeconomicaRequestDTO.class);
            avaliacaoSocioeconomicaService.salvar(fichaCriada.id(), avaliacaoDto);
        }
        if (pendente.getDadosHistoricoJson() != null) {
            HistoricoAtendimentoRequestDTO historicoDto =
                    desserializar(pendente.getDadosHistoricoJson(), HistoricoAtendimentoRequestDTO.class);
            historicoAtendimentoService.salvar(fichaCriada.id(), historicoDto);
        }

        pendente.setStatus(StatusFichaPendente.APROVADA);
        pendente.setFichaId(fichaCriada.id());
        pendente.setRevisadoPorId(usuarioAtual());
        pendente.setDataRevisao(LocalDateTime.now());
        return FichaPendenteResponseDTO.from(fichaPendenteRepository.save(pendente), objectMapper);
    }

    @Transactional
    public FichaPendenteResponseDTO rejeitar(Long id, String motivo) {
        FichaPendente pendente = buscarEntidade(id);
        exigirPendente(pendente);

        pendente.setStatus(StatusFichaPendente.REJEITADA);
        pendente.setMotivoRejeicao(motivo);
        pendente.setRevisadoPorId(usuarioAtual());
        pendente.setDataRevisao(LocalDateTime.now());
        return FichaPendenteResponseDTO.from(fichaPendenteRepository.save(pendente), objectMapper);
    }

    private FichaPendente buscarEntidade(Long id) {
        return fichaPendenteRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("FichaPendente", id));
    }

    private void exigirPendente(FichaPendente pendente) {
        if (pendente.getStatus() != StatusFichaPendente.PENDENTE) {
            throw new IllegalArgumentException("Esta ficha pendente já foi revisada");
        }
    }

    private <T> T desserializar(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar dados da ficha pendente", e);
        }
    }

    private Long usuarioAtual() {
        return CurrentUser.id()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no contexto"));
    }
}
