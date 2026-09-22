package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.response.FichaPendenteResponseDTO;
import com.azizaid.hub.dto.response.FichaResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.FichaPendente;
import com.azizaid.hub.model.enums.StatusFichaPendente;
import com.azizaid.hub.repository.FichaPendenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FichaPendenteService {

    private final FichaPendenteRepository fichaPendenteRepository;
    private final FichaService fichaService;

    public FichaPendenteService(FichaPendenteRepository fichaPendenteRepository, FichaService fichaService) {
        this.fichaPendenteRepository = fichaPendenteRepository;
        this.fichaService = fichaService;
    }

    @Transactional(readOnly = true)
    public List<FichaPendenteResponseDTO> listar(StatusFichaPendente status) {
        StatusFichaPendente filtro = status != null ? status : StatusFichaPendente.PENDENTE;
        return fichaPendenteRepository.findByStatusOrderByDataSubmissaoDesc(filtro).stream()
                .map(FichaPendenteResponseDTO::from)
                .toList();
    }

    @Transactional
    public FichaPendenteResponseDTO aprovar(Long id) {
        FichaPendente pendente = buscarEntidade(id);
        exigirPendente(pendente);

        FichaRequestDTO dto = new FichaRequestDTO(
                null, pendente.getNome(), pendente.getCpf(), pendente.getIdade(), pendente.getTelefone(),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        FichaResponseDTO fichaCriada = fichaService.criar(dto);

        pendente.setStatus(StatusFichaPendente.APROVADA);
        pendente.setFichaId(fichaCriada.id());
        pendente.setRevisadoPorId(usuarioAtual());
        pendente.setDataRevisao(LocalDateTime.now());
        return FichaPendenteResponseDTO.from(fichaPendenteRepository.save(pendente));
    }

    @Transactional
    public FichaPendenteResponseDTO rejeitar(Long id, String motivo) {
        FichaPendente pendente = buscarEntidade(id);
        exigirPendente(pendente);

        pendente.setStatus(StatusFichaPendente.REJEITADA);
        pendente.setMotivoRejeicao(motivo);
        pendente.setRevisadoPorId(usuarioAtual());
        pendente.setDataRevisao(LocalDateTime.now());
        return FichaPendenteResponseDTO.from(fichaPendenteRepository.save(pendente));
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

    private Long usuarioAtual() {
        return CurrentUser.id()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no contexto"));
    }
}
