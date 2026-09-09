package br.edu.tp2.productservice.repository;

import br.edu.tp2.productservice.model.Product;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio em memoria do catalogo de produtos.
 *
 * <p>Nao usa banco de dados: um {@code ConcurrentHashMap} guarda os produtos
 * enquanto a aplicacao estiver no ar. Isso e suficiente para o escopo deste
 * trabalho (ver secao 3 do enunciado) e evita a complexidade de configurar
 * um banco externo neste momento.</p>
 */
@Repository
public class ProductRepository {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public ProductRepository() {
        save(new Product(null, "Teclado Mecanico", 350.00, 20));
        save(new Product(null, "Mouse Gamer", 180.00, 35));
        save(new Product(null, "Monitor 27 polegadas", 1200.00, 10));
    }

    public Product save(Product product) {
        if (product.getId() == null) {
            product.setId(sequence.incrementAndGet());
        }
        products.put(product.getId(), product);
        return product;
    }

    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }
}
