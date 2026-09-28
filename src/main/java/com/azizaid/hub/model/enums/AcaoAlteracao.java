package com.azizaid.hub.model.enums;

// O que foi feito numa linha do entity_audit_log. A criação do caso não entra aqui: ela não é
// gravada no log, sai de ficha.data_criacao (ver FichaAlteracaoService).
public enum AcaoAlteracao {
    EDITOU,
    PREENCHEU,
    MUDOU_STATUS,
    ALTEROU_TIPOS
}
