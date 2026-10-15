package com.umc.pediupatrao.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusPedidoTest {

    @Test
    void fluxoDeEntregaSegueAOrdemEsperada() {
        assertEquals(StatusPedido.EM_PREPARACAO, StatusPedido.RECEBIDO.seguinte(ModoAtendimento.ENTREGA));
        assertEquals(StatusPedido.PRONTO, StatusPedido.EM_PREPARACAO.seguinte(ModoAtendimento.ENTREGA));
        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.PRONTO.seguinte(ModoAtendimento.ENTREGA));
        assertEquals(StatusPedido.FINALIZADO, StatusPedido.SAIU_PARA_ENTREGA.seguinte(ModoAtendimento.ENTREGA));
    }

    @Test
    void fluxoDeRetiradaPassaPorRetirado() {
        assertEquals(StatusPedido.RETIRADO, StatusPedido.PRONTO.seguinte(ModoAtendimento.RETIRADA));
        assertEquals(StatusPedido.FINALIZADO, StatusPedido.RETIRADO.seguinte(ModoAtendimento.RETIRADA));
    }

    @Test
    void finalizadoECanceladoNaoTemProximoStatus() {
        assertNull(StatusPedido.FINALIZADO.seguinte(ModoAtendimento.ENTREGA));
        assertNull(StatusPedido.CANCELADO.seguinte(ModoAtendimento.ENTREGA));
    }

    @Test
    void saidaEAntesDaSaidaSaoExcludentes() {
        assertTrue(StatusPedido.SAIU_PARA_ENTREGA.ehSaida());
        assertTrue(StatusPedido.RETIRADO.ehSaida());
        assertFalse(StatusPedido.PRONTO.ehSaida());

        assertTrue(StatusPedido.RECEBIDO.antesDaSaida());
        assertTrue(StatusPedido.PRONTO.antesDaSaida());
        assertFalse(StatusPedido.SAIU_PARA_ENTREGA.antesDaSaida());
        assertFalse(StatusPedido.FINALIZADO.antesDaSaida());
        assertFalse(StatusPedido.CANCELADO.antesDaSaida());
    }
}
