package com.umc.pediupatrao.auditoria;

public enum AcaoAuditada {
    CRIACAO_CLIENTE("Criação de cliente"),
    ALTERACAO_CLIENTE("Alteração de cliente"),
    EXCLUSAO_CLIENTE("Exclusão de cliente"),
    CRIACAO_PEDIDO("Criação de pedido"),
    ALTERACAO_STATUS("Alteração de status"),
    DESCONTO("Desconto"),
    CANCELAMENTO("Cancelamento");

    private final String descricao;

    AcaoAuditada(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
