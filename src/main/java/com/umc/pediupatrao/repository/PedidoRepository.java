package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    // Início inclusivo e fim exclusivo.
    @Query("{ 'entradaEm': { $gte: ?0, $lt: ?1 } }")
    List<Pedido> findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(LocalDateTime inicio, LocalDateTime fim);
}
