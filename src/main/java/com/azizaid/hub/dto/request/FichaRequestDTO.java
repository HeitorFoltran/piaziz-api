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
import jakarta.validation.constraints.Size;

import java.util.Set;

public record FichaRequestDTO(
        @Size(max = 30, message = "numeroCaso deve ter no máximo 30 caracteres")
        String numeroCaso,

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "cpf é obrigatório")
        @Size(max = 14, message = "cpf deve ter no máximo 14 caracteres")
        String cpf,

        Integer idade,

        @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
        String telefone,

        @Size(max = 40, message = "estadoCivil deve ter no máximo 40 caracteres")
        String estadoCivil,

        Integer pessoasDependentes,

        @Size(max = 120, message = "idadeFilhos deve ter no máximo 120 caracteres")
        String idadeFilhos,

        @Min(value = 0, message = "nivelSeguranca deve ser entre 0 e 5")
        @Max(value = 5, message = "nivelSeguranca deve ser entre 0 e 5")
        Integer nivelSeguranca,

        TipoMoradia tipoMoradia,

        @Size(max = 150, message = "tipoMoradiaOutraDescricao deve ter no máximo 150 caracteres")
        String tipoMoradiaOutraDescricao,

        Integer qtdMoradores,
        Integer qtdFilhos,
        OndeMoramFilhos ondeMoramFilhos,
        SupervisaoFilhos supervisaoFilhos,
        Set<VagaNecessaria> vagasNecessarias,
        Set<NecessidadeImediata> necessidadesImediatas,

        @Size(max = 200, message = "necessidadeOutraDescricao deve ter no máximo 200 caracteres")
        String necessidadeOutraDescricao,

        StatusFicha status
) {
}
