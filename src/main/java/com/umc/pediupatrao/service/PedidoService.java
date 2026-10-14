package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DadosItemPedido;
import com.umc.pediupatrao.dto.DadosNovoPedido;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraDeNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final BigDecimal DESCONTO_MAXIMO = BigDecimal.valueOf(20);

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private AuditoriaService auditoriaService;

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido criarPedido(DadosNovoPedido dados) {
        if (dados.getClienteId() == null || clienteService.buscarPorId(dados.getClienteId()).isEmpty()) {
            throw new IllegalStateException("Escolha um cliente válido para o pedido.");
        }
        if (dados.getTipoAtendimento() == null) {
            throw new IllegalStateException("Informe se o pedido é para entrega ou retirada.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        for (DadosItemPedido dadosItem : dados.getItens()) {
            if (dadosItem == null || dadosItem.getProdutoId() == null || dadosItem.getProdutoId().isBlank()) {
                continue;
            }
            itens.add(montarItem(dadosItem));
        }
        if (itens.isEmpty()) {
            throw new IllegalStateException("O pedido precisa ter pelo menos um item.");
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(dados.getClienteId());
        pedido.setTipoAtendimento(dados.getTipoAtendimento());
        pedido.setItens(itens);
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        pedido.setValorTotal(calcularTotal(itens, BigDecimal.ZERO));
        pedido.setRegistradoPor(usuarioLogado());
        pedido.setEntradaEm(LocalDateTime.now());
        pedido.setStatus(StatusPedido.RECEBIDO.name());

        Pedido gravado = pedidoRepository.save(pedido);

        List<AlteracaoCampo> resumo = List.of(
                new AlteracaoCampo("clienteId", "", gravado.getClienteId()),
                new AlteracaoCampo("tipoAtendimento", "", gravado.getTipoAtendimento().name()),
                new AlteracaoCampo("itens", "", resumirItens(itens)),
                new AlteracaoCampo("valorTotal", "", gravado.getValorTotal().toPlainString()));
        auditoriaService.registrar(TipoOperacao.CRIACAO_PEDIDO, "Pedido", gravado.getId(), resumo, null);

        return gravado;
    }

    private ItemPedido montarItem(DadosItemPedido dadosItem) {
        if (dadosItem.getQuantidade() < 1) {
            throw new IllegalStateException("A quantidade de cada item deve ser de pelo menos 1.");
        }

        Produto produto = produtoService.buscarPorId(dadosItem.getProdutoId())
                .orElseThrow(() -> new IllegalStateException("Produto não encontrado."));
        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new IllegalStateException("O produto " + produto.getNome() + " está inativo.");
        }

        ItemPedido item = new ItemPedido();
        item.setProdutoId(produto.getId());
        item.setNomeProduto(produto.getNome());
        item.setQuantidade(dadosItem.getQuantidade());
        item.setPrecoUnitario(BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    private BigDecimal calcularTotal(List<ItemPedido> itens, BigDecimal percentualDesconto) {
        BigDecimal soma = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            soma = soma.add(item.getSubtotal());
        }
        BigDecimal desconto = soma.multiply(percentualDesconto).divide(BigDecimal.valueOf(100));
        return soma.subtract(desconto).setScale(2, RoundingMode.HALF_UP);
    }

    private String resumirItens(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> item.getQuantidade() + "x " + item.getNomeProduto())
                .collect(Collectors.joining(", "));
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido atualizarStatus(String id, String novoStatus) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Pedido não encontrado."));

        StatusPedido atual = converterStatus(pedido.getStatus());
        StatusPedido desejado = converterStatus(novoStatus);

        if (atual == StatusPedido.CANCELADO) {
            throw new RegraDeNegocioException("Pedido cancelado não pode mais ser alterado.");
        }
        if (atual == StatusPedido.FINALIZADO) {
            throw new RegraDeNegocioException("Pedido finalizado não pode mais ser alterado.");
        }
        if (desejado == StatusPedido.CANCELADO) {
            throw new RegraDeNegocioException("O cancelamento é feito pela operação própria de cancelar pedido.");
        }
        if (atual == StatusPedido.PRONTO && pedido.getTipoAtendimento() == null) {
            throw new RegraDeNegocioException("Pedido sem tipo de atendimento (entrega ou retirada).");
        }
        if (atual.seguinte(pedido.getTipoAtendimento()) != desejado) {
            throw new RegraDeNegocioException("Transição inválida: de " + atual + " para " + desejado + ".");
        }

        List<AlteracaoCampo> mudancas = new ArrayList<>();
        mudancas.add(new AlteracaoCampo("status", atual.name(), desejado.name()));

        pedido.setStatus(desejado.name());
        if (desejado.ehSaida()) {
            LocalDateTime momento = LocalDateTime.now();
            pedido.setSaidaEm(momento);
            pedido.setSaidaPor(usuarioLogado());
            mudancas.add(new AlteracaoCampo("saidaPor", "", pedido.getSaidaPor()));
            mudancas.add(new AlteracaoCampo("saidaEm", "", momento.toString()));
        }

        Pedido gravado = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_STATUS, "Pedido", gravado.getId(), mudancas, null);
        return gravado;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Pedido avancarStatus(String id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Pedido não encontrado."));

        String proximo = proximoStatus(pedido);
        if (proximo == null) {
            throw new RegraDeNegocioException("Este pedido não pode mais avançar.");
        }
        return atualizarStatus(id, proximo);
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        StatusPedido atual = converterStatus(pedido.getStatus());

        if (atual == StatusPedido.CANCELADO) {
            throw new RegraDeNegocioException("Este pedido já está cancelado.");
        }
        if (!atual.antesDaSaida()) {
            throw new RegraDeNegocioException("O pedido só pode ser cancelado antes da saída para entrega ou retirada.");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("Informe a justificativa do cancelamento.");
        }

        pedido.setStatus(StatusPedido.CANCELADO.name());
        Pedido gravado = pedidoRepository.save(pedido);

        List<AlteracaoCampo> mudancas = List.of(
                new AlteracaoCampo("status", atual.name(), StatusPedido.CANCELADO.name()));
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO, "Pedido", gravado.getId(), mudancas, justificativa.trim());
        return gravado;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = buscarPedido(id);
        StatusPedido atual = converterStatus(pedido.getStatus());

        if (percentual == null || percentual.signum() < 0 || percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new RegraDeNegocioException("O desconto deve ficar entre 0% e 20%.");
        }
        if (atual == StatusPedido.CANCELADO) {
            throw new RegraDeNegocioException("Pedido cancelado não aceita desconto.");
        }
        if (!atual.antesDaSaida()) {
            throw new RegraDeNegocioException("O desconto só pode ser aplicado antes da saída para entrega ou retirada.");
        }
        if (pedido.getItens().isEmpty()) {
            throw new RegraDeNegocioException("Pedido sem itens não aceita desconto.");
        }

        BigDecimal percentualAnterior = pedido.getPercentualDesconto();
        BigDecimal totalAnterior = pedido.getValorTotal();
        BigDecimal novoPercentual = percentual.setScale(2, RoundingMode.HALF_UP);

        pedido.setPercentualDesconto(novoPercentual);
        pedido.setValorTotal(calcularTotal(pedido.getItens(), novoPercentual));
        Pedido gravado = pedidoRepository.save(pedido);

        List<AlteracaoCampo> mudancas = List.of(
                new AlteracaoCampo("percentualDesconto", textoDe(percentualAnterior), novoPercentual.toPlainString()),
                new AlteracaoCampo("valorTotal", textoDe(totalAnterior), gravado.getValorTotal().toPlainString()));
        auditoriaService.registrar(TipoOperacao.DESCONTO, "Pedido", gravado.getId(), mudancas, null);
        return gravado;
    }

    public boolean aindaNaoSaiu(Pedido pedido) {
        try {
            return converterStatus(pedido.getStatus()).antesDaSaida();
        } catch (RegraDeNegocioException e) {
            return false;
        }
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Pedido não encontrado."));
    }

    private String textoDe(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }

    public String proximoStatus(Pedido pedido) {
        try {
            StatusPedido atual = converterStatus(pedido.getStatus());
            if (atual == StatusPedido.PRONTO && pedido.getTipoAtendimento() == null) {
                return null;
            }
            StatusPedido proximo = atual.seguinte(pedido.getTipoAtendimento());
            return proximo == null ? null : proximo.name();
        } catch (RegraDeNegocioException e) {
            return null;
        }
    }

    private StatusPedido converterStatus(String texto) {
        try {
            return StatusPedido.valueOf(texto);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraDeNegocioException("Status inválido: " + texto);
        }
    }

    private String usuarioLogado() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}
