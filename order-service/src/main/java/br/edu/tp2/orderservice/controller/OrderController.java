package br.edu.tp2.orderservice.controller;

import br.edu.tp2.orderservice.client.ProductClient;
import br.edu.tp2.orderservice.client.ProductDTO;
import br.edu.tp2.orderservice.model.Order;
import br.edu.tp2.orderservice.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API REST de pedidos.
 *
 * <p>Endpoints exigidos pelo enunciado: {@code GET /orders},
 * {@code POST /orders} e {@code GET /orders/{id}}.</p>
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository repository;
    private final ProductClient productClient;

    public OrderController(OrderRepository repository, ProductClient productClient) {
        this.repository = repository;
        this.productClient = productClient;
    }

    @GetMapping
    public List<Order> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cria um pedido. Antes de salvar, consulta o product-service (secao 7)
     * para confirmar que o produto existe — e essa chamada, sozinha, e a
     * prova pratica de que os dois microsservicos se comunicam.
     */
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Order pedido) {
        Optional<ProductDTO> produto = productClient.buscarProduto(pedido.getProductId());

        if (produto.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "erro", "Produto nao encontrado no product-service",
                            "productId", pedido.getProductId()
                    ));
        }

        pedido.setStatus("CONFIRMADO");
        Order salvo = repository.save(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
}
