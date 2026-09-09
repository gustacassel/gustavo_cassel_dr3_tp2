package br.edu.tp2.orderservice.repository;

import br.edu.tp2.orderservice.model.Order;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio em memoria dos pedidos, no mesmo espirito do
 * {@code ProductRepository} do product-service (ver secao 4 do enunciado).
 */
@Repository
public class OrderRepository {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(sequence.incrementAndGet());
        }
        orders.put(order.getId(), order);
        return order;
    }

    public List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orders.get(id));
    }
}
