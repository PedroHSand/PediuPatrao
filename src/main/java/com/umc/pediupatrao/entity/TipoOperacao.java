package com.umc.pediupatrao.entity;

public enum TipoOperacao {
    CRIACAO_CLIENTE("Criação de cliente"),
    ALTERACAO_CLIENTE("Alteração de cliente"),
    EXCLUSAO_CLIENTE("Exclusão de cliente"),
    CRIACAO_PEDIDO("Criação de pedido"),
    ALTERACAO_STATUS("Alteração de status"),
    DESCONTO("Desconto"),
    CANCELAMENTO("Cancelamento");

    private final String descricao;

    TipoOperacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
