package com.azizaid.hub.service;

import com.azizaid.hub.dto.response.DashboardStatsDTO;
import com.azizaid.hub.dto.response.DashboardStatsDTO.EncaminhamentoPorTipo;
import com.azizaid.hub.dto.response.DashboardStatsDTO.FichasPorMes;
import com.azizaid.hub.model.enums.StatusFicha;
import com.azizaid.hub.repository.EncaminhamentoRepository;
import com.azizaid.hub.repository.FichaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private static final int MESES_NO_GRAFICO = 6;
    private static final int DIAS_SEM_ATUALIZACAO = 30;

    private static final String[] MESES_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final FichaRepository fichaRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;

    public DashboardService(FichaRepository fichaRepository,
                            EncaminhamentoRepository encaminhamentoRepository) {
        this.fichaRepository = fichaRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDTO obterEstatisticas() {
        LocalDate hoje = LocalDate.now();

        long totalFichasAtivas = fichaRepository.countByStatus(StatusFicha.ATIVO);
        long totalEncaminhamentos = encaminhamentoRepository.count();
        long fichasAguardandoRetorno = encaminhamentoRepository.contarFichasAguardandoRetorno(hoje);
        long fichasSemAtualizacao = fichaRepository.countByStatusAndDataAtualizacaoBefore(
                StatusFicha.ATIVO, LocalDateTime.now().minusDays(DIAS_SEM_ATUALIZACAO));

        return new DashboardStatsDTO(
                totalFichasAtivas,
                totalEncaminhamentos,
                fichasAguardandoRetorno,
                fichasSemAtualizacao,
                montarEncaminhamentosPorServico(),
                montarFichasPorMes()
        );
    }

    private List<EncaminhamentoPorTipo> montarEncaminhamentosPorServico() {
        return encaminhamentoRepository.contarPorServico().stream()
                .map(linha -> new EncaminhamentoPorTipo(
                        String.valueOf(linha[0]),
                        (String) linha[1],
                        (Long) linha[2]
                ))
                .toList();
    }

    private List<FichasPorMes> montarFichasPorMes() {
        List<FichasPorMes> lista = new ArrayList<>();
        YearMonth atual = YearMonth.now();

        for (int i = MESES_NO_GRAFICO - 1; i >= 0; i--) {
            YearMonth ym = atual.minusMonths(i);
            LocalDateTime inicio = ym.atDay(1).atStartOfDay();
            LocalDateTime fim = ym.plusMonths(1).atDay(1).atStartOfDay();

            long total = fichaRepository.countByDataCriacaoBetween(inicio, fim);
            lista.add(new FichasPorMes(MESES_PT[ym.getMonthValue() - 1], ym.getYear(), total));
        }
        return lista;
    }
}