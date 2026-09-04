package com.azizaid.hub.dto.response;

import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.NecessidadeImediata;
import com.azizaid.hub.model.enums.VagaNecessaria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record FichaResponseDTO(
        Long id,
        String codigoFicha,
        String numeroCaso,
        String nome,
        String cpf,
        Integer idade,
        String telefone,
        String estadoCivil,
        Integer pessoasDependentes,
        String idadeFilhos,
        Integer nivelSeguranca,
        String tipoMoradia,
        String tipoMoradiaOutraDescricao,
        Integer qtdMoradores,
        Integer qtdFilhos,
        String ondeMoramFilhos,
        String supervisaoFilhos,
        Set<String> vagasNecessarias,
        Set<String> necessidadesImediatas,
        String necessidadeOutraDescricao,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        String status,
        List<EncaminhamentoResponseDTO> encaminhamentos,
        List<InteracaoResponseDTO> interacoes,
        AvaliacaoSocioeconomicaResponseDTO avaliacaoSocioeconomica,
        HistoricoAtendimentoResponseDTO historicoAtendimento,
        AcolhimentoEquipeResponseDTO acolhimentoEquipe
) {
    public static FichaResponseDTO from(Ficha f) {
        List<EncaminhamentoResponseDTO> enc = f.getEncaminhamentos() == null ? List.of()
                : f.getEncaminhamentos().stream().map(EncaminhamentoResponseDTO::from).toList();
        List<InteracaoResponseDTO> inter = f.getInteracoes() == null ? List.of()
                : f.getInteracoes().stream().map(InteracaoResponseDTO::from).toList();
        Set<String> vagas = f.getVagasNecessarias() == null ? Set.of()
                : f.getVagasNecessarias().stream().map(VagaNecessaria::name).collect(Collectors.toSet());
        Set<String> necessidades = f.getNecessidadesImediatas() == null ? Set.of()
                : f.getNecessidadesImediatas().stream().map(NecessidadeImediata::name).collect(Collectors.toSet());
        return new FichaResponseDTO(
                f.getId(),
                f.getCodigoFicha(),
                f.getNumeroCaso(),
                f.getNome(),
                f.getCpf(),
                f.getIdade(),
                f.getTelefone(),
                f.getEstadoCivil(),
                f.getPessoasDependentes(),
                f.getIdadeFilhos(),
                f.getNivelSeguranca(),
                f.getTipoMoradia() != null ? f.getTipoMoradia().name() : null,
                f.getTipoMoradiaOutraDescricao(),
                f.getQtdMoradores(),
                f.getQtdFilhos(),
                f.getOndeMoramFilhos() != null ? f.getOndeMoramFilhos().name() : null,
                f.getSupervisaoFilhos() != null ? f.getSupervisaoFilhos().name() : null,
                vagas,
                necessidades,
                f.getNecessidadeOutraDescricao(),
                f.getDataCriacao(),
                f.getDataAtualizacao(),
                f.getStatus() != null ? f.getStatus().name() : null,
                enc,
                inter,
                AvaliacaoSocioeconomicaResponseDTO.from(f.getAvaliacaoSocioeconomica()),
                HistoricoAtendimentoResponseDTO.from(f.getHistoricoAtendimento()),
                AcolhimentoEquipeResponseDTO.from(f.getAcolhimentoEquipe())
        );
    }
}
