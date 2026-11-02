package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;

public record ItemRanking(String produto, String categoria, long quantidade, BigDecimal faturamento) {
}
