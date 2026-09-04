package com.azizaid.hub.service;

import com.azizaid.hub.dto.request.AvaliacaoSocioeconomicaRequestDTO;
import com.azizaid.hub.dto.response.AvaliacaoSocioeconomicaResponseDTO;
import com.azizaid.hub.model.AvaliacaoSocioeconomica;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.repository.AvaliacaoSocioeconomicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoSocioeconomicaService {

    private final AvaliacaoSocioeconomicaRepository avaliacaoSocioeconomicaRepository;
    private final FichaService fichaService;
    private final EntityAuditService entityAuditService;

    public AvaliacaoSocioeconomicaService(AvaliacaoSocioeconomicaRepository avaliacaoSocioeconomicaRepository,
                                          FichaService fichaService,
                                          EntityAuditService entityAuditService) {
        this.avaliacaoSocioeconomicaRepository = avaliacaoSocioeconomicaRepository;
        this.fichaService = fichaService;
        this.entityAuditService = entityAuditService;
    }

    @Transactional(readOnly = true)
    public AvaliacaoSocioeconomicaResponseDTO buscarPorFicha(Long fichaId) {
        fichaService.buscarEntidade(fichaId);
        return avaliacaoSocioeconomicaRepository.findByFichaId(fichaId)
                .map(AvaliacaoSocioeconomicaResponseDTO::from)
                .orElse(null);
    }

    @Transactional
    public AvaliacaoSocioeconomicaResponseDTO salvar(Long fichaId, AvaliacaoSocioeconomicaRequestDTO dto) {
        Ficha ficha = fichaService.buscarEntidade(fichaId);
        AvaliacaoSocioeconomica avaliacao = avaliacaoSocioeconomicaRepository.findByFichaId(fichaId)
                .orElseGet(() -> {
                    AvaliacaoSocioeconomica nova = new AvaliacaoSocioeconomica();
                    nova.setFicha(ficha);
                    return nova;
                });

        if (avaliacao.getId() != null) {
            Long donoId = avaliacao.getCriadoPorId() != null ? avaliacao.getCriadoPorId() : avaliacao.getUltimoEditorId();
            entityAuditService.registrarSeCrossUser("AvaliacaoSocioeconomica", avaliacao.getId(), donoId);
        }

        avaliacao.setTemRenda(dto.temRenda());
        avaliacao.setValorRenda(dto.valorRenda());
        avaliacao.setPessoasDependemRenda(dto.pessoasDependemRenda());
        avaliacao.setOrigemRenda(dto.origemRenda());
        avaliacao.setTrabalhoFormal(dto.trabalhoFormal());
        avaliacao.setRendaSuficiente(dto.rendaSuficiente());
        avaliacao.setTrabalhandoAtualmente(dto.trabalhandoAtualmente());
        avaliacao.setOndeTrabalha(dto.ondeTrabalha());
        avaliacao.setProblemaSaudeAtrapalhaTrabalho(dto.problemaSaudeAtrapalhaTrabalho());
        avaliacao.setProblemaSaudeQual(dto.problemaSaudeQual());
        avaliacao.setSituacaoFamiliarAtrapalhaTrabalho(dto.situacaoFamiliarAtrapalhaTrabalho());
        avaliacao.setSituacaoFamiliarQual(dto.situacaoFamiliarQual());
        avaliacao.setDesejaTrabalhar(dto.desejaTrabalhar());
        avaliacao.setPeriodoDesejado(dto.periodoDesejado());
        avaliacao.setSabeLer(dto.sabeLer());
        avaliacao.setNivelEscrita(dto.nivelEscrita());
        avaliacao.setNivelEscolaridade(dto.nivelEscolaridade());
        avaliacao.setEscolaridadeDetalhe(dto.escolaridadeDetalhe());
        avaliacao.setFezCursoProfissionalizante(dto.fezCursoProfissionalizante());
        avaliacao.setCursoProfissionalizanteQual(dto.cursoProfissionalizanteQual());
        avaliacao.setDesejaAuxilioCeebja(dto.desejaAuxilioCeebja());
        avaliacao.setDesejaCursoSenai(dto.desejaCursoSenai());
        avaliacao.setAreaCursoSenai(dto.areaCursoSenai());
        avaliacao.setTemRedeApoio(dto.temRedeApoio());
        avaliacao.setPrecisaAjudaMoradia(dto.precisaAjudaMoradia());
        avaliacao.setTemOQueComer(dto.temOQueComer());
        avaliacao.setAcompanhamentoMedico(dto.acompanhamentoMedico());
        avaliacao.setPrecisaAjudaTratamentoMedico(dto.precisaAjudaTratamentoMedico());
        avaliacao.setUsoContinuoMedicamento(dto.usoContinuoMedicamento());
        avaliacao.setMedicamentoQuais(dto.medicamentoQuais());
        avaliacao.setAcessoMedicamentos(dto.acessoMedicamentos());

        return AvaliacaoSocioeconomicaResponseDTO.from(avaliacaoSocioeconomicaRepository.save(avaliacao));
    }
}
