package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.ServicoRequestDTO;
import com.azizaid.hub.dto.response.ServicoResponseDTO;
import com.azizaid.hub.exception.RecursoNaoEncontradoException;
import com.azizaid.hub.model.Servico;
import com.azizaid.hub.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ServicoService {

    private static final String MENSAGEM_NOME_DUPLICADO = "Já existe um serviço com este nome";

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> listar() {
        Set<Long> emUso = new HashSet<>(servicoRepository.idsEmUso());
        return servicoRepository.findAll().stream()
                .map(s -> ServicoResponseDTO.from(s, emUso.contains(s.getId())))
                .toList();
    }

    // Sem unique index no banco, de propósito: cadastro de serviço é raro e feito por poucas pessoas,
    // então a checagem aqui basta. A index entra junto com o Flyway, se um dia for preciso.
    @Transactional
    public ServicoResponseDTO criar(ServicoRequestDTO dto) {
        String nome = dto.nome().trim();
        if (servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException(MENSAGEM_NOME_DUPLICADO);
        }
        Servico servico = new Servico();
        servico.setNome(nome);
        return ServicoResponseDTO.from(servicoRepository.save(servico), false);
    }

    @Transactional
    public ServicoResponseDTO atualizar(Long id, ServicoRequestDTO dto) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", id));

        String nome = dto.nome().trim();
        if (!servico.getNome().equalsIgnoreCase(nome) && servicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new IllegalArgumentException(MENSAGEM_NOME_DUPLICADO);
        }

        servico.setNome(nome);
        return ServicoResponseDTO.from(servicoRepository.save(servico), emUso(id));
    }

    // Só exclui serviço que nunca foi usado. Um serviço com encaminhamento ou profissional ligado
    // faz parte do histórico dos casos e não pode sumir.
    @Transactional
    public void excluir(Long id) {
        Servico servico = servicoRepository.buscarParaExclusao(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", id));

        long encaminhamentos = servicoRepository.contarEncaminhamentos(id);
        long profissionais = servicoRepository.contarProfissionais(id);
        if (encaminhamentos > 0 || profissionais > 0) {
            List<String> usos = new ArrayList<>();
            if (encaminhamentos > 0) usos.add(encaminhamentos + " encaminhamento(s)");
            if (profissionais > 0) usos.add(profissionais + " profissional(is)");
            throw new IllegalArgumentException("Não é possível excluir: o serviço está em uso em "
                    + String.join(" e ", usos) + ".");
        }
        servicoRepository.delete(servico);
    }

    private boolean emUso(Long id) {
        return servicoRepository.contarEncaminhamentos(id) > 0 || servicoRepository.contarProfissionais(id) > 0;
    }
}
