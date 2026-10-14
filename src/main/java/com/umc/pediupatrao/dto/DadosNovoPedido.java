package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoAtendimento;

import java.util.ArrayList;
import java.util.List;

public class DadosNovoPedido {

    private String clienteId;
    private TipoAtendimento tipoAtendimento;
    private List<DadosItemPedido> itens = new ArrayList<>();

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public List<DadosItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<DadosItemPedido> itens) {
        this.itens = itens;
    }
}
