package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dashboard.FiltroInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class ErrosDaApi {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> regraDeNegocio(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(FiltroInvalidoException.class)
    public ResponseEntity<String> filtroInvalido(FiltroInvalidoException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<String> parametrosInvalidos(BindException e) {
        String campo = e.getFieldError() != null ? e.getFieldError().getField() : "desconhecido";
        return ResponseEntity.badRequest().body("Valor inválido para o parâmetro '" + campo + "'.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> naoEncontrado(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body("Valor inválido para o parâmetro '" + e.getName() + "'.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body("Corpo da requisição inválido.");
    }
}
