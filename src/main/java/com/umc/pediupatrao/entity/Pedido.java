package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private String status;
    private int quantidade;
    private String sabor;
    private List<ItemDoPedido> itens = new ArrayList<>();
    private ModoAtendimento tipoAtendimento;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal valorTotal;
    private String registradoPor;
    private LocalDateTime entradaEm;
    private LocalDateTime saidaEm;
    private String saidaPor;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoPizza() {
        return tipoPizza;
    }

    public void setTipoPizza(String tipoPizza) {
        this.tipoPizza = tipoPizza;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getSabor() {
        return sabor;
    }

    public void setSabor(String sabor) {
        this.sabor = sabor;
    }

    public List<ItemDoPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemDoPedido> itens) {
        this.itens = itens;
    }

    public ModoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(ModoAtendimento tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(BigDecimal percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }

    public LocalDateTime getEntradaEm() {
        return entradaEm;
    }

    public void setEntradaEm(LocalDateTime entradaEm) {
        this.entradaEm = entradaEm;
    }

    public LocalDateTime getSaidaEm() {
        return saidaEm;
    }

    public void setSaidaEm(LocalDateTime saidaEm) {
        this.saidaEm = saidaEm;
    }

    public String getSaidaPor() {
        return saidaPor;
    }

    public void setSaidaPor(String saidaPor) {
        this.saidaPor = saidaPor;
    }
    
    
}
