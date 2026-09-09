package br.edu.tp2.productservice.controller;

import br.edu.tp2.productservice.model.Product;
import br.edu.tp2.productservice.repository.ProductRepository;
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
 * Testa a API REST do catalogo (secao 3 do enunciado) via MockMvc, sem
 * subir um servidor HTTP real — o repositorio e mockado, entao o teste
 * verifica apenas o contrato do Controller (rota, verbo, status, JSON).
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository repository;

    @Test
    void deveListarProdutos() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                new Product(1L, "Teclado Mecanico", 350.0, 20),
                new Product(2L, "Mouse Gamer", 180.0, 35)
        ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Teclado Mecanico"));
    }

    @Test
    void deveBuscarProdutoExistentePorId() throws Exception {
        when(repository.findById(1L)).thenReturn(
                Optional.of(new Product(1L, "Teclado Mecanico", 350.0, 20)));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Teclado Mecanico"));
    }

    @Test
    void deveRetornar404ParaProdutoInexistente() throws Exception {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarNovoProduto() throws Exception {
        when(repository.save(any(Product.class))).thenAnswer(invocation -> {
            Product produto = invocation.getArgument(0);
            produto.setId(10L);
            return produto;
        });

        String corpo = """
                {"name":"Cadeira Gamer","price":899.90,"quantity":5}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Cadeira Gamer"));
    }

    @Test
    void endpointDeInstanciaDeveDevolverHostname() throws Exception {
        mockMvc.perform(get("/products/instance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname").exists());
    }
}
