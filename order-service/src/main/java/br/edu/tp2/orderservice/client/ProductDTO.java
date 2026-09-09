package br.edu.tp2.orderservice.client;

/**
 * Representacao do produto tal como o {@code product-service} devolve.
 * So os campos que o order-service realmente usa.
 */
public record ProductDTO(Long id, String name, double price, int quantity) {
}
