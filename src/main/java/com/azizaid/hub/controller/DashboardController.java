package com.azizaid.hub.controller;

import com.azizaid.hub.dto.response.DashboardStatsDTO;
import com.azizaid.hub.dto.response.RelatorioAnaliticoDTO;
import com.azizaid.hub.service.DashboardService;
import com.azizaid.hub.service.RelatorioAnaliticoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('PADRAO','DEV')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final RelatorioAnaliticoService relatorioAnaliticoService;

    public DashboardController(DashboardService dashboardService,
                                RelatorioAnaliticoService relatorioAnaliticoService) {
        this.dashboardService = dashboardService;
        this.relatorioAnaliticoService = relatorioAnaliticoService;
    }

    @GetMapping("/stats")
    public DashboardStatsDTO stats() {
        return dashboardService.obterEstatisticas();
    }

    @GetMapping("/relatorios")
    public RelatorioAnaliticoDTO relatorios(@RequestParam(required = false) String granularidade) {
        return relatorioAnaliticoService.gerar(granularidade);
    }
}
