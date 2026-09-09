package br.edu.tp2.orderservice.model;

/**
 * Representa um pedido feito na loja.
 *
 * <p>{@code status} comeca como {@code PENDENTE} e vira {@code CONFIRMADO}
 * somente depois que o {@code order-service} confirma junto ao
 * {@code product-service} que o produto pedido existe (ver
 * {@code OrderController#criar}).</p>
 */
public class Order {

    private Long id;
    private Long productId;
    private int quantity;
    private String status;

    public Order() {
    }

    public Order(Long id, Long productId, int quantity, String status) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
