package com.umc.pediupatrao.entity;

public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusPedido seguinte(ModoAtendimento tipo) {
        switch (this) {
            case RECEBIDO:
                return EM_PREPARACAO;
            case EM_PREPARACAO:
                return PRONTO;
            case PRONTO:
                return tipo == ModoAtendimento.ENTREGA ? SAIU_PARA_ENTREGA : RETIRADO;
            case SAIU_PARA_ENTREGA:
            case RETIRADO:
                return FINALIZADO;
            default:
                return null;
        }
    }

    public boolean ehSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
