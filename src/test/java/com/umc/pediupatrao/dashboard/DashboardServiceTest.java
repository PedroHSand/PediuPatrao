package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.ItemDoPedido;
import com.umc.pediupatrao.entity.ModoAtendimento;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private PedidoRepository pedidoRepository;
    private ProdutoRepository produtoRepository;
    private DashboardService service;

    // "Hoje" nos testes é quarta-feira, 04/11/2026. A semana começa na segunda, 02/11.
    private static final ParametrosFiltro HOJE = new ParametrosFiltro(null, null, null, null, null, null);

    @BeforeEach
    void montarCenario() {
        pedidoRepository = mock(PedidoRepository.class);
        produtoRepository = mock(ProdutoRepository.class);

        Clock relogio = Clock.fixed(Instant.parse("2026-11-04T18:00:00Z"), FUSO);
        service = new DashboardService(pedidoRepository, produtoRepository, relogio);

        when(produtoRepository.findAll()).thenReturn(List.of(
                produto("p1", "Calabresa", "Pizza"),
                produto("p2", "Refrigerante", "Bebida")));

        List<Pedido> todos = List.of(
                // hoje, em preparo, entrega
                pedido(LocalDateTime.of(2026, 11, 4, 12, 10), StatusPedido.EM_PREPARACAO,
                        ModoAtendimento.ENTREGA, "100.00", "0", item("p1", "Calabresa", 2, "50.00")),
                // hoje, finalizado, retirada
                pedido(LocalDateTime.of(2026, 11, 4, 20, 30), StatusPedido.FINALIZADO,
                        ModoAtendimento.RETIRADA, "60.00", "0",
                        item("p2", "Refrigerante", 2, "5.00"), item("p1", "Calabresa", 1, "50.00")),
                // hoje, cancelado: não conta como venda
                pedido(LocalDateTime.of(2026, 11, 4, 13, 0), StatusPedido.CANCELADO,
                        ModoAtendimento.ENTREGA, "50.00", "0", item("p1", "Calabresa", 1, "50.00")),
                // segunda, saiu para entrega, com 10% de desconto
                pedido(LocalDateTime.of(2026, 11, 2, 19, 0), StatusPedido.SAIU_PARA_ENTREGA,
                        ModoAtendimento.ENTREGA, "90.00", "10", item("p1", "Calabresa", 2, "50.00")),
                // domingo, semana anterior mas ainda no mês
                pedido(LocalDateTime.of(2026, 11, 1, 12, 0), StatusPedido.PRONTO,
                        ModoAtendimento.RETIRADA, "50.00", "0", item("p1", "Calabresa", 1, "50.00")));

        when(pedidoRepository.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(any(), any()))
                .thenAnswer(chamada -> {
                    LocalDateTime de = chamada.getArgument(0);
                    LocalDateTime ate = chamada.getArgument(1);
                    return todos.stream()
                            .filter(p -> !p.getEntradaEm().isBefore(de) && p.getEntradaEm().isBefore(ate))
                            .toList();
                });
    }

    // ---------- indicadores ----------

    @Test
    void resumoDeHojeIgnoraPedidoCancelado() {
        ResumoDashboard resumo = service.resumo(HOJE);

        assertEquals(2, resumo.pedidos());
        assertEquals(new BigDecimal("160.00"), resumo.faturamento());
        assertEquals(new BigDecimal("80.00"), resumo.ticketMedio());
        assertEquals(1, resumo.emPreparo());
        assertEquals(1, resumo.jaSairam());
        assertEquals("Calabresa", resumo.produtoMaisVendido());
    }

    @Test
    void resumoTrazTotaisFixosDeHojeSemanaEMes() {
        ResumoDashboard resumo = service.resumo(HOJE);

        assertEquals(2, resumo.hoje().pedidos());
        assertEquals(new BigDecimal("160.00"), resumo.hoje().faturamento());
        assertEquals(3, resumo.semana().pedidos());
        assertEquals(new BigDecimal("250.00"), resumo.semana().faturamento());
        assertEquals(4, resumo.mes().pedidos());
        assertEquals(new BigDecimal("300.00"), resumo.mes().faturamento());
    }

    @Test
    void totaisFixosNaoMudamQuandoOFiltroMuda() {
        ResumoDashboard resumo = service.resumo(
                new ParametrosFiltro(null, null, null, ModoAtendimento.ENTREGA, null, null));

        assertEquals(1, resumo.pedidos());
        assertEquals(2, resumo.hoje().pedidos());
    }

    @Test
    void resumoSemPedidosNaoQuebraENaoTemProdutoMaisVendido() {
        ResumoDashboard resumo = service.resumo(
                new ParametrosFiltro(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), null, null, null));

        assertEquals(0, resumo.pedidos());
        assertEquals(new BigDecimal("0.00"), resumo.faturamento());
        assertEquals(new BigDecimal("0.00"), resumo.ticketMedio());
        assertNull(resumo.produtoMaisVendido());
    }

    // ---------- gráficos ----------

    @Test
    void faturamentoPorDiaIncluiDiasSemVenda() {
        List<PontoSerie> serie = service.vendasPorDia(
                new ParametrosFiltro(Periodo.MES, null, null, null, null, null));

        assertEquals(4, serie.size());
        assertEquals("2026-11-01", serie.get(0).rotulo());
        assertEquals(new BigDecimal("50.00"), serie.get(0).faturamento());
        assertEquals(new BigDecimal("90.00"), serie.get(1).faturamento());
        assertEquals(0, serie.get(2).pedidos());
        assertEquals(new BigDecimal("0.00"), serie.get(2).faturamento());
        assertEquals(2, serie.get(3).pedidos());
        assertEquals(new BigDecimal("160.00"), serie.get(3).faturamento());
    }

    @Test
    void pedidosPorHoraTem24PontosEAgrupaPelaHoraDeEntrada() {
        List<PontoSerie> serie = service.vendasPorHora(HOJE);

        assertEquals(24, serie.size());
        assertEquals("12h", serie.get(12).rotulo());
        assertEquals(1, serie.get(12).pedidos());
        assertEquals(new BigDecimal("100.00"), serie.get(12).faturamento());
        assertEquals(1, serie.get(20).pedidos());
        assertEquals(0, serie.get(13).pedidos());
    }

    @Test
    void vendasPorCategoriaSomamOFaturamentoDoPeriodo() {
        List<PontoSerie> serie = service.vendasPorCategoria(HOJE);

        assertEquals(2, serie.size());
        assertEquals("Pizza", serie.get(0).rotulo());
        assertEquals(2, serie.get(0).pedidos());
        assertEquals(new BigDecimal("150.00"), serie.get(0).faturamento());
        assertEquals("Bebida", serie.get(1).rotulo());
        assertEquals(new BigDecimal("10.00"), serie.get(1).faturamento());
    }

    @Test
    void categoriaConsideraODescontoDoPedido() {
        List<PontoSerie> serie = service.vendasPorCategoria(
                new ParametrosFiltro(Periodo.SEMANA, null, null, null, null, "pizza"));

        assertEquals(1, serie.size());
        assertEquals(new BigDecimal("240.00"), serie.get(0).faturamento());
    }

    @Test
    void entregaERetiradaSempreVoltamAsDuasOpcoes() {
        List<PontoSerie> serie = service.vendasPorAtendimento(HOJE);

        assertEquals("Entrega", serie.get(0).rotulo());
        assertEquals(1, serie.get(0).pedidos());
        assertEquals(new BigDecimal("100.00"), serie.get(0).faturamento());
        assertEquals("Retirada", serie.get(1).rotulo());
        assertEquals(new BigDecimal("60.00"), serie.get(1).faturamento());
    }

    // ---------- situação e ranking ----------

    @Test
    void situacaoDosPedidosContaTodosOsStatusInclusiveCancelado() {
        List<SituacaoStatus> situacao = service.situacaoDosPedidos(HOJE);

        assertEquals(StatusPedido.values().length, situacao.size());
        assertEquals(1, quantidade(situacao, "EM_PREPARACAO"));
        assertEquals(1, quantidade(situacao, "FINALIZADO"));
        assertEquals(1, quantidade(situacao, "CANCELADO"));
        assertEquals(0, quantidade(situacao, "RECEBIDO"));
    }

    @Test
    void rankingOrdenaPorQuantidadeELimitaOResultado() {
        List<ItemRanking> ranking = service.maisVendidos(
                new ParametrosFiltro(Periodo.SEMANA, null, null, null, null, null), 5);

        assertEquals(2, ranking.size());
        assertEquals("Calabresa", ranking.get(0).produto());
        assertEquals(5, ranking.get(0).quantidade());
        assertEquals(new BigDecimal("240.00"), ranking.get(0).faturamento());
        assertEquals("Refrigerante", ranking.get(1).produto());

        assertEquals(1, service.maisVendidos(HOJE, 1).size());
    }

    @Test
    void rankingPodeSerFiltradoPorCategoria() {
        List<ItemRanking> ranking = service.maisVendidos(
                new ParametrosFiltro(null, null, null, null, null, "Bebida"), 5);

        assertEquals(1, ranking.size());
        assertEquals("Refrigerante", ranking.get(0).produto());
        assertEquals(2, ranking.get(0).quantidade());
    }

    // ---------- filtros ----------

    @Test
    void filtraPorStatus() {
        ResumoDashboard resumo = service.resumo(
                new ParametrosFiltro(null, null, null, null, StatusPedido.FINALIZADO, null));

        assertEquals(1, resumo.pedidos());
        assertEquals(new BigDecimal("60.00"), resumo.faturamento());
    }

    @Test
    void periodoPersonalizadoTemPrioridadeSobreOPeriodoFixo() {
        ResumoDashboard resumo = service.resumo(new ParametrosFiltro(
                Periodo.HOJE, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), null, null, null));

        assertEquals(2, resumo.pedidos());
        assertEquals(LocalDate.of(2026, 11, 1), resumo.inicio());
    }

    @Test
    void dataInicialDepoisDaFinalEhRecusada() {
        assertThrows(FiltroInvalidoException.class, () -> service.resumo(new ParametrosFiltro(
                null, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 1), null, null, null)));
    }

    @Test
    void informarSoUmaDataEhRecusado() {
        assertThrows(FiltroInvalidoException.class, () -> service.resumo(new ParametrosFiltro(
                null, LocalDate.of(2026, 11, 1), null, null, null, null)));
    }

    @Test
    void periodoMuitoLongoEhRecusado() {
        assertThrows(FiltroInvalidoException.class, () -> service.resumo(new ParametrosFiltro(
                null, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1), null, null, null)));
    }

    @Test
    void limiteDoRankingForaDaFaixaEhRecusado() {
        assertThrows(FiltroInvalidoException.class, () -> service.maisVendidos(HOJE, 0));
        assertThrows(FiltroInvalidoException.class, () -> service.maisVendidos(HOJE, 21));
    }

    @Test
    void pedidoAntigoSemItensENemTotalNaoQuebraOsCalculos() {
        Pedido antigo = new Pedido();
        antigo.setEntradaEm(LocalDateTime.of(2026, 11, 4, 9, 0));
        antigo.setStatus(StatusPedido.RECEBIDO.name());
        antigo.setItens(null);
        doReturn(List.of(antigo)).when(pedidoRepository)
                .findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(any(), any());

        ResumoDashboard resumo = service.resumo(HOJE);

        assertEquals(1, resumo.pedidos());
        assertEquals(new BigDecimal("0.00"), resumo.faturamento());
        assertNull(resumo.produtoMaisVendido());
        assertEquals(0, service.vendasPorCategoria(HOJE).size());
    }

    @Test
    void categoriasVemDosProdutosSemRepetirEmOrdemAlfabetica() {
        when(produtoRepository.findAll()).thenReturn(List.of(
                produto("p1", "Calabresa", "Pizza"),
                produto("p2", "Coca-Cola", "Bebida"),
                produto("p3", "Pepsi", "Bebida"),
                produto("p4", "Esfiha de carne", "Esfiha")));

        assertEquals(List.of("Bebida", "Esfiha", "Pizza"), service.categorias());
    }

    // ---------- apoio ----------

    private long quantidade(List<SituacaoStatus> situacao, String status) {
        return situacao.stream().filter(s -> s.status().equals(status)).findFirst().orElseThrow().quantidade();
    }

    private Produto produto(String id, String nome, String tipo) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setTipo(tipo);
        return produto;
    }

    private ItemDoPedido item(String produtoId, String nome, int quantidade, String preco) {
        ItemDoPedido item = new ItemDoPedido();
        item.setProdutoId(produtoId);
        item.setNomeProduto(nome);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(new BigDecimal(preco));
        return item;
    }

    private Pedido pedido(LocalDateTime entrada, StatusPedido status, ModoAtendimento tipo,
            String total, String desconto, ItemDoPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setEntradaEm(entrada);
        pedido.setStatus(status.name());
        pedido.setTipoAtendimento(tipo);
        pedido.setValorTotal(new BigDecimal(total));
        pedido.setPercentualDesconto(new BigDecimal(desconto));
        pedido.setItens(List.of(itens));
        return pedido;
    }
}
