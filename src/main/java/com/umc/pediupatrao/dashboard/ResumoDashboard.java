package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoDashboard(
        LocalDate inicio,
        LocalDate fim,
        long pedidos,
        BigDecimal faturamento,
        BigDecimal ticketMedio,
        long emPreparo,
        long jaSairam,
        String produtoMaisVendido,
        TotalPeriodo hoje,
        TotalPeriodo semana,
        TotalPeriodo mes) {

    // Totais fixos de hoje, da semana e do mês, que não dependem do filtro escolhido.
    public record TotalPeriodo(long pedidos, BigDecimal faturamento) {
    }
}
