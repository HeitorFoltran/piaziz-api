package com.azizaid.hub.dto.response;

import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.request.FichaRequestDTO;
import com.azizaid.hub.dto.request.HistoricoAtendimentoRequestDTO;
import com.azizaid.hub.model.FichaPendente;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public record FichaPendenteResponseDTO(
        Long id,
        String nome,
        String cpf,
        String telefone,
        Integer idade,
        String situacaoRelatada,
        FichaRequestDTO ficha,
        AvaliacaoSocioeconomicaRequestDTO avaliacao,
        HistoricoAtendimentoRequestDTO historico,
        LocalDateTime dataSubmissao,
        String status,
        Long revisadoPorId,
        LocalDateTime dataRevisao,
        String motivoRejeicao,
        Long fichaId
) {
    public static FichaPendenteResponseDTO from(FichaPendente f, ObjectMapper objectMapper) {
        return new FichaPendenteResponseDTO(
                f.getId(),
                f.getNome(),
                f.getCpf(),
                f.getTelefone(),
                f.getIdade(),
                f.getSituacaoRelatada(),
                desserializar(objectMapper, f.getDadosFichaJson(), FichaRequestDTO.class),
                desserializar(objectMapper, f.getDadosAvaliacaoJson(), AvaliacaoSocioeconomicaRequestDTO.class),
                desserializar(objectMapper, f.getDadosHistoricoJson(), HistoricoAtendimentoRequestDTO.class),
                f.getDataSubmissao(),
                f.getStatus() != null ? f.getStatus().name() : null,
                f.getRevisadoPorId(),
                f.getDataRevisao(),
                f.getMotivoRejeicao(),
                f.getFichaId());
    }

    private static <T> T desserializar(ObjectMapper objectMapper, String json, Class<T> tipo) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar dados da ficha pendente", e);
        }
    }
}
