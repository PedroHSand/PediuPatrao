package com.umc.pediupatrao.service;

import com.umc.pediupatrao.auditoria.AuditoriaService;
import com.umc.pediupatrao.dto.NovoItemForm;
import com.umc.pediupatrao.dto.NovoPedidoForm;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemDoPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.ModoAtendimento;
import com.umc.pediupatrao.auditoria.AcaoAuditada;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private ProdutoService produtoService;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void entrarComoBalcao() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "balcao", "x", List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"))));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private Produto produto(String id, String nome, double preco, boolean ativo) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }

    private NovoItemForm item(String produtoId, int quantidade) {
        NovoItemForm item = new NovoItemForm();
        item.setProdutoId(produtoId);
        item.setQuantidade(quantidade);
        return item;
    }

    private NovoPedidoForm pedidoDe(NovoItemForm... itens) {
        NovoPedidoForm dados = new NovoPedidoForm();
        dados.setClienteId("c1");
        dados.setTipoAtendimento(ModoAtendimento.ENTREGA);
        dados.setItens(new ArrayList<>(List.of(itens)));
        return dados;
    }

    private Pedido pedidoNoStatus(StatusPedido status, ModoAtendimento tipo) {
        Pedido pedido = new Pedido();
        pedido.setId("p1");
        pedido.setStatus(status.name());
        pedido.setTipoAtendimento(tipo);

        ItemDoPedido item = new ItemDoPedido();
        item.setProdutoId("prod1");
        item.setNomeProduto("Calabresa");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("50.00"));
        pedido.setItens(new ArrayList<>(List.of(item)));
        pedido.setValorTotal(new BigDecimal("100.00"));
        return pedido;
    }

    private void devolverPedido(Pedido pedido) {
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
    }

    private void gravarSemAlterar() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    // ---------- criação do pedido ----------

    @Test
    void criarPedidoCalculaTotalPrecoResponsavelEEntradaNoServidor() {
        when(clienteService.buscarPorId("c1")).thenReturn(Optional.of(new Cliente()));
        when(produtoService.buscarPorId("a")).thenReturn(Optional.of(produto("a", "Calabresa", 45.90, true)));
        when(produtoService.buscarPorId("b")).thenReturn(Optional.of(produto("b", "Refrigerante", 8.00, true)));
        gravarSemAlterar();

        Pedido pedido = pedidoService.criarPedido(pedidoDe(item("a", 2), item("b", 1)));

        assertEquals(new BigDecimal("99.80"), pedido.getValorTotal());
        assertEquals(new BigDecimal("45.90"), pedido.getItens().get(0).getPrecoUnitario());
        assertEquals("RECEBIDO", pedido.getStatus());
        assertEquals("balcao", pedido.getRegistradoPor());
        assertNotNull(pedido.getEntradaEm());
        verify(auditoriaService).registrar(eq(AcaoAuditada.CRIACAO_PEDIDO), eq("Pedido"), any(), anyList(), isNull());
    }

    @Test
    void criarPedidoRecusaQuantidadeMenorQueUm() {
        when(clienteService.buscarPorId("c1")).thenReturn(Optional.of(new Cliente()));

        assertThrows(IllegalStateException.class, () -> pedidoService.criarPedido(pedidoDe(item("a", 0))));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedidoRecusaProdutoInativo() {
        when(clienteService.buscarPorId("c1")).thenReturn(Optional.of(new Cliente()));
        when(produtoService.buscarPorId("a")).thenReturn(Optional.of(produto("a", "Calabresa", 45.90, false)));

        assertThrows(IllegalStateException.class, () -> pedidoService.criarPedido(pedidoDe(item("a", 1))));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedidoRecusaPedidoSemItens() {
        when(clienteService.buscarPorId("c1")).thenReturn(Optional.of(new Cliente()));

        assertThrows(IllegalStateException.class, () -> pedidoService.criarPedido(pedidoDe(item("", 1))));
        verify(pedidoRepository, never()).save(any());
    }

    // ---------- fluxo de status ----------

    @Test
    void statusAvancaUmPassoDeCadaVez() {
        Pedido pedido = pedidoNoStatus(StatusPedido.RECEBIDO, ModoAtendimento.ENTREGA);
        devolverPedido(pedido);
        gravarSemAlterar();

        pedidoService.atualizarStatus("p1", "EM_PREPARACAO");

        assertEquals("EM_PREPARACAO", pedido.getStatus());
    }

    @Test
    void statusNaoPermitePularEtapa() {
        devolverPedido(pedidoNoStatus(StatusPedido.EM_PREPARACAO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "FINALIZADO"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void statusNaoPermiteVoltar() {
        devolverPedido(pedidoNoStatus(StatusPedido.PRONTO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "RECEBIDO"));
    }

    @Test
    void cancelamentoNaoPassaPelaTrocaComumDeStatus() {
        devolverPedido(pedidoNoStatus(StatusPedido.RECEBIDO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "CANCELADO"));
    }

    @Test
    void saidaDaEntregaRegistraHoraEResponsavel() {
        Pedido pedido = pedidoNoStatus(StatusPedido.PRONTO, ModoAtendimento.ENTREGA);
        devolverPedido(pedido);
        gravarSemAlterar();

        pedidoService.atualizarStatus("p1", "SAIU_PARA_ENTREGA");

        assertEquals("SAIU_PARA_ENTREGA", pedido.getStatus());
        assertEquals("balcao", pedido.getSaidaPor());
        assertNotNull(pedido.getSaidaEm());
    }

    @Test
    void retiradaVaiParaRetiradoEExigeASaida() {
        Pedido pedido = pedidoNoStatus(StatusPedido.PRONTO, ModoAtendimento.RETIRADA);
        devolverPedido(pedido);
        gravarSemAlterar();

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "SAIU_PARA_ENTREGA"));

        pedidoService.atualizarStatus("p1", "RETIRADO");
        assertEquals("RETIRADO", pedido.getStatus());
        assertNotNull(pedido.getSaidaEm());
    }

    @Test
    void pedidoFinalizadoNaoMudaMais() {
        devolverPedido(pedidoNoStatus(StatusPedido.FINALIZADO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "EM_PREPARACAO"));
    }

    @Test
    void pedidoCanceladoNaoMudaMais() {
        devolverPedido(pedidoNoStatus(StatusPedido.CANCELADO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "EM_PREPARACAO"));
    }

    // ---------- cancelamento ----------

    @Test
    void cancelarExigeJustificativa() {
        devolverPedido(pedidoNoStatus(StatusPedido.RECEBIDO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.cancelar("p1", "   "));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelarAntesDaSaidaPreservaOPedidoEAudita() {
        Pedido pedido = pedidoNoStatus(StatusPedido.EM_PREPARACAO, ModoAtendimento.ENTREGA);
        devolverPedido(pedido);
        gravarSemAlterar();

        pedidoService.cancelar("p1", "Cliente desistiu");

        assertEquals("CANCELADO", pedido.getStatus());
        assertEquals(1, pedido.getItens().size());
        assertEquals(new BigDecimal("100.00"), pedido.getValorTotal());
        verify(auditoriaService).registrar(eq(AcaoAuditada.CANCELAMENTO), eq("Pedido"), any(), anyList(),
                eq("Cliente desistiu"));
    }

    @Test
    void cancelarDepoisDaSaidaEhBloqueado() {
        devolverPedido(pedidoNoStatus(StatusPedido.SAIU_PARA_ENTREGA, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.cancelar("p1", "tarde demais"));
    }

    @Test
    void cancelarPedidoJaCanceladoEhBloqueado() {
        devolverPedido(pedidoNoStatus(StatusPedido.CANCELADO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.cancelar("p1", "de novo"));
    }

    // ---------- desconto ----------

    @Test
    void descontoRecalculaOTotalESubstituiOAnterior() {
        Pedido pedido = pedidoNoStatus(StatusPedido.RECEBIDO, ModoAtendimento.ENTREGA);
        devolverPedido(pedido);
        gravarSemAlterar();

        pedidoService.aplicarDesconto("p1", new BigDecimal("10"));
        assertEquals(new BigDecimal("90.00"), pedido.getValorTotal());

        pedidoService.aplicarDesconto("p1", new BigDecimal("5"));
        assertEquals(new BigDecimal("95.00"), pedido.getValorTotal());
        assertEquals(new BigDecimal("5.00"), pedido.getPercentualDesconto());
    }

    @Test
    void descontoAceitaOLimiteDeVintePorCento() {
        Pedido pedido = pedidoNoStatus(StatusPedido.PRONTO, ModoAtendimento.ENTREGA);
        devolverPedido(pedido);
        gravarSemAlterar();

        pedidoService.aplicarDesconto("p1", new BigDecimal("20"));

        assertEquals(new BigDecimal("80.00"), pedido.getValorTotal());
    }

    @Test
    void descontoAcimaDeVintePorCentoEhRecusado() {
        devolverPedido(pedidoNoStatus(StatusPedido.RECEBIDO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("20.01")));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void descontoNegativoEhRecusado() {
        devolverPedido(pedidoNoStatus(StatusPedido.RECEBIDO, ModoAtendimento.ENTREGA));

        assertThrows(IllegalStateException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("-1")));
    }

    @Test
    void descontoDepoisDaSaidaOuEmCanceladoEhBloqueado() {
        devolverPedido(pedidoNoStatus(StatusPedido.RETIRADO, ModoAtendimento.RETIRADA));
        assertThrows(IllegalStateException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("5")));

        devolverPedido(pedidoNoStatus(StatusPedido.CANCELADO, ModoAtendimento.ENTREGA));
        assertThrows(IllegalStateException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("5")));
    }
}
