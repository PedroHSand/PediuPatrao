package com.umc.pediupatrao.exception;

public class NegocioException extends IllegalStateException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
