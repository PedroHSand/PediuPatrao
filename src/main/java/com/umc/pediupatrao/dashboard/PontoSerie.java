package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;

// Um ponto de gráfico: rótulo do eixo, quantidade de pedidos e valor faturado.
public record PontoSerie(String rotulo, long pedidos, BigDecimal faturamento) {
}
