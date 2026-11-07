package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.ModoAtendimento;
import com.umc.pediupatrao.entity.StatusPedido;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Parâmetros de consulta aceitos por todos os endpoints do dashboard.
public record ParametrosFiltro(
        Periodo periodo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
        ModoAtendimento tipoAtendimento,
        StatusPedido status,
        String categoria) {
}
