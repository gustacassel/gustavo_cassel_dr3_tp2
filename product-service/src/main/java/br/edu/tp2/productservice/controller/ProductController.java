package br.edu.tp2.productservice.controller;

import br.edu.tp2.productservice.model.Product;
import br.edu.tp2.productservice.repository.ProductRepository;
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

/**
 * API REST do catalogo de produtos.
 *
 * <p>Endpoints exigidos pelo enunciado: {@code GET /products},
 * {@code GET /products/{id}} e {@code POST /products}.</p>
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Product> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> criar(@RequestBody Product product) {
        Product salvo = repository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    /**
     * Endpoint auxiliar, nao exigido pelo enunciado: devolve o hostname do
     * pod que atendeu a chamada. Serve como evidencia pratica de
     * balanceamento de carga (secao 13) — ao chamar repetidas vezes atraves
     * do Service do Kubernetes com varias replicas, hostnames diferentes
     * devem aparecer nas respostas.
     */
    @GetMapping("/instance")
    public Map<String, String> instancia() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "desconhecido");
        return Map.of("hostname", hostname);
    }
}
