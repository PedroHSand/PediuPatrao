package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.ModoAtendimento;

import java.util.ArrayList;
import java.util.List;

public class NovoPedidoForm {

    private String clienteId;
    private ModoAtendimento tipoAtendimento;
    private List<NovoItemForm> itens = new ArrayList<>();

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public ModoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(ModoAtendimento tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public List<NovoItemForm> getItens() {
        return itens;
    }

    public void setItens(List<NovoItemForm> itens) {
        this.itens = itens;
    }
}
