package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.ModoAtendimento;
import com.umc.pediupatrao.entity.StatusPedido;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

// Filtro já validado, com o intervalo de datas resolvido.
public record FiltroDashboard(
        LocalDate inicio,
        LocalDate fim,
        ModoAtendimento tipoAtendimento,
        StatusPedido status,
        String categoria) {

    static final long MAXIMO_DIAS = 366;

    public static FiltroDashboard de(ParametrosFiltro parametros, LocalDate hoje) {
        LocalDate inicio;
        LocalDate fim;

        if (parametros.inicio() != null || parametros.fim() != null) {
            if (parametros.inicio() == null || parametros.fim() == null) {
                throw new FiltroInvalidoException("Informe as duas datas, inicio e fim.");
            }
            inicio = parametros.inicio();
            fim = parametros.fim();
        } else {
            Periodo periodo = parametros.periodo() == null ? Periodo.HOJE : parametros.periodo();
            inicio = inicioDo(periodo, hoje);
            fim = hoje;
        }

        if (inicio.isAfter(fim)) {
            throw new FiltroInvalidoException("A data inicial não pode ser depois da data final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > MAXIMO_DIAS) {
            throw new FiltroInvalidoException("O período máximo de consulta é de " + MAXIMO_DIAS + " dias.");
        }

        String categoria = parametros.categoria() == null || parametros.categoria().isBlank()
                ? null
                : parametros.categoria().trim();

        return new FiltroDashboard(inicio, fim, parametros.tipoAtendimento(), parametros.status(), categoria);
    }

    public static LocalDate inicioDo(Periodo periodo, LocalDate hoje) {
        switch (periodo) {
            case SEMANA:
                return hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES:
                return hoje.withDayOfMonth(1);
            default:
                return hoje;
        }
    }
}
