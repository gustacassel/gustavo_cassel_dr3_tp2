package br.edu.tp2.orderservice.controller;

import br.edu.tp2.orderservice.client.ProductClient;
import br.edu.tp2.orderservice.client.ProductDTO;
import br.edu.tp2.orderservice.model.Order;
import br.edu.tp2.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a API REST de pedidos (secao 4) via MockMvc. O {@code ProductClient}
 * e mockado, entao este teste tambem cobre a regra da secao 7 (o pedido so
 * e confirmado se o produto existir no product-service) sem precisar do
 * product-service de verdade no ar.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository repository;

    @MockBean
    private ProductClient productClient;

    @Test
    void deveConfirmarPedidoQuandoProdutoExisteNoProductService() throws Exception {
        when(productClient.buscarProduto(1L)).thenReturn(
                Optional.of(new ProductDTO(1L, "Teclado Mecanico", 350.0, 20)));
        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order pedido = invocation.getArgument(0);
            pedido.setId(100L);
            return pedido;
        });

        String corpo = """
                {"productId":1,"quantity":2}
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void deveRejeitarPedidoQuandoProdutoNaoExisteNoProductService() throws Exception {
        when(productClient.buscarProduto(999L)).thenReturn(Optional.empty());

        String corpo = """
                {"productId":999,"quantity":1}
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarPedidos() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                new Order(1L, 1L, 2, "CONFIRMADO")
        ));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveBuscarPedidoExistentePorId() throws Exception {
        when(repository.findById(1L)).thenReturn(
                Optional.of(new Order(1L, 1L, 2, "CONFIRMADO")));

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void deveRetornar404ParaPedidoInexistente() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/999"))
                .andExpect(status().isNotFound());
    }
}
