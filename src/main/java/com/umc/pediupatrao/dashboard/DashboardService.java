package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.ItemDoPedido;
import com.umc.pediupatrao.entity.ModoAtendimento;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final String SEM_CATEGORIA = "Outros";
    private static final int LIMITE_MAXIMO_RANKING = 20;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final Clock relogio;

    @Autowired
    public DashboardService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
        this(pedidoRepository, produtoRepository, Clock.system(FUSO));
    }

    DashboardService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository, Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.relogio = relogio;
    }

    public ResumoDashboard resumo(ParametrosFiltro parametros) {
        FiltroDashboard filtro = montarFiltro(parametros);
        List<Pedido> vendas = selecionar(filtro, false);

        BigDecimal faturamento = somarTotais(vendas);
        BigDecimal ticketMedio = vendas.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(vendas.size()), 2, RoundingMode.HALF_UP);

        long emPreparo = vendas.stream().filter(p -> statusDe(p) == StatusPedido.EM_PREPARACAO).count();
        long jaSairam = vendas.stream().filter(this::jaSaiu).count();

        List<ItemRanking> lider = ranking(vendas, filtro, 1);
        String maisVendido = lider.isEmpty() ? null : lider.get(0).produto();

        LocalDate hoje = hoje();
        return new ResumoDashboard(filtro.inicio(), filtro.fim(), vendas.size(), faturamento, ticketMedio,
                emPreparo, jaSairam, maisVendido,
                totalDoPeriodo(Periodo.HOJE, hoje),
                totalDoPeriodo(Periodo.SEMANA, hoje),
                totalDoPeriodo(Periodo.MES, hoje));
    }

    public List<PontoSerie> vendasPorDia(ParametrosFiltro parametros) {
        FiltroDashboard filtro = montarFiltro(parametros);

        Map<LocalDate, long[]> contagem = new TreeMap<>();
        Map<LocalDate, BigDecimal> valores = new HashMap<>();
        for (LocalDate dia = filtro.inicio(); !dia.isAfter(filtro.fim()); dia = dia.plusDays(1)) {
            contagem.put(dia, new long[1]);
            valores.put(dia, BigDecimal.ZERO);
        }

        for (Pedido pedido : selecionar(filtro, false)) {
            LocalDate dia = pedido.getEntradaEm().toLocalDate();
            contagem.get(dia)[0]++;
            valores.merge(dia, totalDe(pedido), BigDecimal::add);
        }

        List<PontoSerie> serie = new ArrayList<>();
        contagem.forEach((dia, qtd) -> serie.add(new PontoSerie(dia.toString(), qtd[0], escala(valores.get(dia)))));
        return serie;
    }

    public List<PontoSerie> vendasPorHora(ParametrosFiltro parametros) {
        FiltroDashboard filtro = montarFiltro(parametros);

        long[] pedidos = new long[24];
        BigDecimal[] valores = new BigDecimal[24];
        for (int hora = 0; hora < 24; hora++) {
            valores[hora] = BigDecimal.ZERO;
        }

        for (Pedido pedido : selecionar(filtro, false)) {
            int hora = pedido.getEntradaEm().getHour();
            pedidos[hora]++;
            valores[hora] = valores[hora].add(totalDe(pedido));
        }

        List<PontoSerie> serie = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            serie.add(new PontoSerie(String.format("%02dh", hora), pedidos[hora], escala(valores[hora])));
        }
        return serie;
    }

    public List<PontoSerie> vendasPorCategoria(ParametrosFiltro parametros) {
        FiltroDashboard filtro = montarFiltro(parametros);
        Map<String, String> categorias = categoriasPorProduto();

        Map<String, Long> pedidosPorCategoria = new HashMap<>();
        Map<String, BigDecimal> valorPorCategoria = new HashMap<>();

        for (Pedido pedido : selecionar(filtro, false)) {
            Map<String, BigDecimal> doPedido = new HashMap<>();
            for (ItemDoPedido item : itensDe(pedido)) {
                String categoria = categoriaDe(item, categorias);
                if (!combina(categoria, filtro.categoria())) {
                    continue;
                }
                doPedido.merge(categoria, valorDoItem(item, pedido), BigDecimal::add);
            }
            doPedido.forEach((categoria, valor) -> {
                pedidosPorCategoria.merge(categoria, 1L, Long::sum);
                valorPorCategoria.merge(categoria, valor, BigDecimal::add);
            });
        }

        List<PontoSerie> serie = new ArrayList<>();
        valorPorCategoria.forEach((categoria, valor) ->
                serie.add(new PontoSerie(categoria, pedidosPorCategoria.get(categoria), escala(valor))));
        serie.sort(Comparator.comparing(PontoSerie::faturamento).reversed());
        return serie;
    }

    public List<PontoSerie> vendasPorAtendimento(ParametrosFiltro parametros) {
        FiltroDashboard filtro = montarFiltro(parametros);

        Map<ModoAtendimento, Long> pedidos = new LinkedHashMap<>();
        Map<ModoAtendimento, BigDecimal> valores = new LinkedHashMap<>();
        for (ModoAtendimento modo : ModoAtendimento.values()) {
            pedidos.put(modo, 0L);
            valores.put(modo, BigDecimal.ZERO);
        }

        for (Pedido pedido : selecionar(filtro, false)) {
            ModoAtendimento modo = pedido.getTipoAtendimento();
            if (modo == null) {
                continue;
            }
            pedidos.merge(modo, 1L, Long::sum);
            valores.merge(modo, totalDe(pedido), BigDecimal::add);
        }

        List<PontoSerie> serie = new ArrayList<>();
        for (ModoAtendimento modo : ModoAtendimento.values()) {
            String rotulo = modo.name().charAt(0) + modo.name().substring(1).toLowerCase();
            serie.add(new PontoSerie(rotulo, pedidos.get(modo), escala(valores.get(modo))));
        }
        return serie;
    }

    public List<SituacaoStatus> situacaoDosPedidos(ParametrosFiltro parametros) {
        FiltroDashboard filtro = montarFiltro(parametros);

        Map<StatusPedido, Long> contagem = new LinkedHashMap<>();
        for (StatusPedido status : StatusPedido.values()) {
            contagem.put(status, 0L);
        }
        for (Pedido pedido : selecionar(filtro, true)) {
            StatusPedido status = statusDe(pedido);
            if (status != null) {
                contagem.merge(status, 1L, Long::sum);
            }
        }

        List<SituacaoStatus> situacao = new ArrayList<>();
        contagem.forEach((status, qtd) -> situacao.add(new SituacaoStatus(status.name(), status.getDescricao(), qtd)));
        return situacao;
    }

    public List<ItemRanking> maisVendidos(ParametrosFiltro parametros, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO_RANKING) {
            throw new FiltroInvalidoException("O limite deve ficar entre 1 e " + LIMITE_MAXIMO_RANKING + ".");
        }
        FiltroDashboard filtro = montarFiltro(parametros);
        return ranking(selecionar(filtro, false), filtro, limite);
    }

    // Tipos de produto cadastrados, para montar o filtro de categoria da tela.
    public List<String> categorias() {
        return categoriasPorProduto().values().stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    // ---------- apoio ----------

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private FiltroDashboard montarFiltro(ParametrosFiltro parametros) {
        return FiltroDashboard.de(parametros, hoje());
    }

    // Busca os pedidos do intervalo e aplica os demais filtros. Cancelados só entram quando pedido.
    private List<Pedido> selecionar(FiltroDashboard filtro, boolean incluirCancelados) {
        LocalDateTime de = filtro.inicio().atStartOfDay();
        LocalDateTime ate = filtro.fim().plusDays(1).atStartOfDay();
        Map<String, String> categorias = filtro.categoria() == null ? Map.of() : categoriasPorProduto();

        return pedidoRepository.findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(de, ate).stream()
                .filter(p -> p.getEntradaEm() != null)
                .filter(p -> incluirCancelados || statusDe(p) != StatusPedido.CANCELADO)
                .filter(p -> filtro.status() == null || statusDe(p) == filtro.status())
                .filter(p -> filtro.tipoAtendimento() == null || p.getTipoAtendimento() == filtro.tipoAtendimento())
                .filter(p -> filtro.categoria() == null || itensDe(p).stream()
                        .anyMatch(i -> combina(categoriaDe(i, categorias), filtro.categoria())))
                .toList();
    }

    private ResumoDashboard.TotalPeriodo totalDoPeriodo(Periodo periodo, LocalDate hoje) {
        FiltroDashboard filtro = new FiltroDashboard(FiltroDashboard.inicioDo(periodo, hoje), hoje, null, null, null);
        List<Pedido> vendas = selecionar(filtro, false);
        return new ResumoDashboard.TotalPeriodo(vendas.size(), somarTotais(vendas));
    }

    private List<ItemRanking> ranking(List<Pedido> vendas, FiltroDashboard filtro, int limite) {
        Map<String, String> categorias = categoriasPorProduto();
        Map<String, String> nomes = new HashMap<>();
        Map<String, String> categoriaDoProduto = new HashMap<>();
        Map<String, Long> quantidades = new HashMap<>();
        Map<String, BigDecimal> valores = new HashMap<>();

        for (Pedido pedido : vendas) {
            for (ItemDoPedido item : itensDe(pedido)) {
                String categoria = categoriaDe(item, categorias);
                if (!combina(categoria, filtro.categoria())) {
                    continue;
                }
                String chave = item.getProdutoId() != null ? item.getProdutoId() : item.getNomeProduto();
                nomes.putIfAbsent(chave, item.getNomeProduto());
                categoriaDoProduto.putIfAbsent(chave, categoria);
                quantidades.merge(chave, (long) item.getQuantidade(), Long::sum);
                valores.merge(chave, valorDoItem(item, pedido), BigDecimal::add);
            }
        }

        List<ItemRanking> ranking = new ArrayList<>();
        quantidades.forEach((chave, qtd) -> ranking.add(
                new ItemRanking(nomes.get(chave), categoriaDoProduto.get(chave), qtd, escala(valores.get(chave)))));
        ranking.sort(Comparator.comparingLong(ItemRanking::quantidade).reversed()
                .thenComparing(Comparator.comparing(ItemRanking::faturamento).reversed()));
        return ranking.stream().limit(limite).toList();
    }

    private Map<String, String> categoriasPorProduto() {
        Map<String, String> categorias = new HashMap<>();
        for (Produto produto : produtoRepository.findAll()) {
            if (produto.getId() != null && produto.getTipo() != null) {
                categorias.put(produto.getId(), produto.getTipo());
            }
        }
        return categorias;
    }

    private String categoriaDe(ItemDoPedido item, Map<String, String> categorias) {
        return categorias.getOrDefault(item.getProdutoId(), SEM_CATEGORIA);
    }

    private boolean combina(String categoria, String filtro) {
        return filtro == null || categoria.equalsIgnoreCase(filtro);
    }

    private List<ItemDoPedido> itensDe(Pedido pedido) {
        return pedido.getItens() == null ? List.of() : pedido.getItens();
    }

    private StatusPedido statusDe(Pedido pedido) {
        try {
            return StatusPedido.valueOf(pedido.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private boolean jaSaiu(Pedido pedido) {
        StatusPedido status = statusDe(pedido);
        return status != null && (status.ehSaida() || status == StatusPedido.FINALIZADO);
    }

    private BigDecimal totalDe(Pedido pedido) {
        return pedido.getValorTotal() == null ? BigDecimal.ZERO : pedido.getValorTotal();
    }

    private BigDecimal somarTotais(List<Pedido> pedidos) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Pedido pedido : pedidos) {
            soma = soma.add(totalDe(pedido));
        }
        return escala(soma);
    }

    // Valor do item já com o desconto do pedido, para que as categorias somem o mesmo que o faturamento.
    private BigDecimal valorDoItem(ItemDoPedido item, Pedido pedido) {
        if (item.getPrecoUnitario() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal desconto = pedido.getPercentualDesconto() == null ? BigDecimal.ZERO : pedido.getPercentualDesconto();
        BigDecimal fator = BigDecimal.ONE.subtract(desconto.divide(BigDecimal.valueOf(100)));
        return item.getSubtotal().multiply(fator);
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
