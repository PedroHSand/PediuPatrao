package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.NovoPedidoForm;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<?> criarPedido(@RequestBody NovoPedidoForm dados) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(dados));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable String id, @RequestParam String justificativa) {
        try {
            return ResponseEntity.ok(pedidoService.cancelar(id, justificativa));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/desconto")
    public ResponseEntity<?> aplicarDesconto(@PathVariable String id, @RequestParam BigDecimal percentual) {
        try {
            return ResponseEntity.ok(pedidoService.aplicarDesconto(id, percentual));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(@PathVariable String id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(pedidoService.atualizarStatus(id, status));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
    
    
}
