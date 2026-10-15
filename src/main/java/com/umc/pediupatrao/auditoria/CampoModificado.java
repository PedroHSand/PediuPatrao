package com.umc.pediupatrao.auditoria;

public class CampoModificado {

    private final String campo;
    private final String valorAnterior;
    private final String valorNovo;

    public CampoModificado(String campo, String valorAnterior, String valorNovo) {
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }
}
