package com.azizaid.hub.service;

import com.azizaid.hub.config.CurrentUser;
import com.azizaid.hub.model.EntityAuditLog;
import com.azizaid.hub.model.Ficha;
import com.azizaid.hub.model.enums.AcaoAlteracao;
import com.azizaid.hub.repository.EntityAuditLogRepository;
import com.azizaid.hub.util.RetratoAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntityAuditService {

    // Uma parte preenchida logo depois de criar o caso (Nova Ficha, aprovação de ficha pendente) já
    // está coberta pela linha "criou o caso" do histórico.
    static final long JANELA_CRIACAO_MINUTOS = 10;

    private final EntityAuditLogRepository entityAuditLogRepository;

    public EntityAuditService(EntityAuditLogRepository entityAuditLogRepository) {
        this.entityAuditLogRepository = entityAuditLogRepository;
    }

    // Grava qualquer que seja o editor; quem chama decide se houve alteração. Nunca passar valor de
    // campo em detalhe: o log não pode virar mais um lugar com dado da vítima.
    @Transactional
    public void registrar(String tipoEntidade, Long entidadeId, Long donoId, Long fichaId,
                          AcaoAlteracao acao, String detalhe) {
        Long editorId = CurrentUser.id().orElse(null);
        if (editorId == null) {
            return;
        }
        Long dono = donoId != null ? donoId : editorId;
        entityAuditLogRepository.save(EntityAuditLog.builder()
                .tipoEntidade(tipoEntidade)
                .entidadeId(entidadeId)
                .fichaId(fichaId)
                .editorId(editorId)
                .donoId(dono)
                .acao(acao)
                .detalhe(detalhe)
                .resumo("profissional " + editorId + " " + acao + " " + tipoEntidade + " #" + entidadeId)
                .build());
    }

    // Para as partes do caso salvas por upsert (avaliação, acolhimento, histórico de atendimento).
    // Registro que já existia: EDITOU se o retrato mudou. Registro novo: PREENCHEU só se veio algum
    // campo preenchido e o caso não foi criado agora há pouco.
    @Transactional
    public void registrarSalvamentoDeParte(String tipoEntidade, Long entidadeId, Long donoId, Ficha ficha,
                                           boolean novo, List<Object> antes, List<Object> depois) {
        if (!novo) {
            if (RetratoAuditoria.mudou(antes, depois)) {
                registrar(tipoEntidade, entidadeId, donoId, ficha.getId(), AcaoAlteracao.EDITOU, null);
            }
            return;
        }
        if (RetratoAuditoria.algumPreenchido(depois) && foraDaJanelaDeCriacao(ficha)) {
            registrar(tipoEntidade, entidadeId, donoId, ficha.getId(), AcaoAlteracao.PREENCHEU, null);
        }
    }

    private static boolean foraDaJanelaDeCriacao(Ficha ficha) {
        return ficha.getDataCriacao() != null
                && ficha.getDataCriacao().isBefore(LocalDateTime.now().minusMinutes(JANELA_CRIACAO_MINUTOS));
    }
}
