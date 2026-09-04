package com.azizaid.hub.dto.request;

import com.azizaid.hub.model.enums.NecessidadeImediata;
import com.azizaid.hub.model.enums.OndeMoramFilhos;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.model.enums.SupervisaoFilhos;
import com.azizaid.hub.model.enums.TipoMoradia;
import com.azizaid.hub.model.enums.VagaNecessaria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record FichaRequestDTO(
        String numeroCaso,

        @NotBlank(message = "nome é obrigatório")
        String nome,

        @NotBlank(message = "cpf é obrigatório")
        String cpf,

        Integer idade,
        String telefone,
        String estadoCivil,
        Integer pessoasDependentes,
        String idadeFilhos,

        @Min(value = 0, message = "nivelSeguranca deve ser entre 0 e 5")
        @Max(value = 5, message = "nivelSeguranca deve ser entre 0 e 5")
        Integer nivelSeguranca,

        TipoMoradia tipoMoradia,
        String tipoMoradiaOutraDescricao,
        Integer qtdMoradores,
        Integer qtdFilhos,
        OndeMoramFilhos ondeMoramFilhos,
        SupervisaoFilhos supervisaoFilhos,
        Set<VagaNecessaria> vagasNecessarias,
        Set<NecessidadeImediata> necessidadesImediatas,
        String necessidadeOutraDescricao,
        StatusFicha status
) {
}
