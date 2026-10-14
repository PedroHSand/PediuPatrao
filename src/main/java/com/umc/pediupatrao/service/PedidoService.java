package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DadosItemPedido;
import com.umc.pediupatrao.dto.DadosNovoPedido;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoOperacao;
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
        pedido.setRegistradoPor(SecurityContextHolder.getContext().getAuthentication().getName());
        pedido.setEntradaEm(LocalDateTime.now());
        pedido.setStatus("RECEBIDO");

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

    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }

}
