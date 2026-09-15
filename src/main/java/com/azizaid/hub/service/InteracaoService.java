package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.dto.request.InteracaoRequestDTO;
import com.azizaid.hub.dto.response.InteracaoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.Interacao;
import com.azizaid.hub.model.Profissional;
import com.azizaid.hub.repository.InteracaoRepository;
import com.azizaid.hub.repository.ProfissionalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InteracaoService {

    private final InteracaoRepository interacaoRepository;
    private final FichaService fichaService;
    private final ProfissionalRepository profissionalRepository;

    public InteracaoService(InteracaoRepository interacaoRepository, FichaService fichaService,
                             ProfissionalRepository profissionalRepository) {
        this.interacaoRepository = interacaoRepository;
        this.fichaService = fichaService;
        this.profissionalRepository = profissionalRepository;
    }

    @Transactional(readOnly = true)
    public List<InteracaoResponseDTO> listarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        return interacaoRepository.findByFichaIdOrderByDataInteracaoDescIdDesc(fichaId).stream()
                .map(InteracaoResponseDTO::from)
                .toList();
    }

    @Transactional
    public InteracaoResponseDTO criar(Long fichaId, InteracaoRequestDTO dto) {
        Ficha ficha = fichaService.buscarEntidade(fichaId);

        Long autorId = CurrentUser.id()
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no contexto"));
        Profissional autor = profissionalRepository.findById(autorId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Profissional", autorId));

        Interacao interacao = new Interacao();
        interacao.setFicha(ficha);
        interacao.setAutor(autor.getNome());
        interacao.setTexto(dto.texto());
        interacao.setDataInteracao(LocalDateTime.now());

        Interacao salvo = interacaoRepository.save(interacao);

        ficha.setDataAtualizacao(LocalDateTime.now());

        return InteracaoResponseDTO.from(salvo);
    }
}
