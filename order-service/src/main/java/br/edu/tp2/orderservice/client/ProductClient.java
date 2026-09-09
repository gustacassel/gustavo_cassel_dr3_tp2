package br.edu.tp2.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Cliente HTTP que consulta o product-service (secao 7 do enunciado).
 *
 * <p>O endereco vem da propriedade {@code product.service.url}, que por
 * padrao aponta para {@code localhost} (util so para rodar os dois servicos
 * direto na maquina, fora de container). Em Docker Compose e em Kubernetes
 * essa propriedade e sobrescrita pela variavel de ambiente
 * {@code PRODUCT_SERVICE_URL}, apontando para o <b>nome do container /
 * nome do Service</b> — nunca para {@code localhost}, exatamente como o
 * enunciado exige.</p>
 */
@Component
public class ProductClient {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public ProductClient(RestTemplate restTemplate,
                          @Value("${product.service.url}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }

    public Optional<ProductDTO> buscarProduto(Long id) {
        try {
            ProductDTO produto = restTemplate.getForObject(
                    productServiceUrl + "/products/" + id, ProductDTO.class);
            return Optional.ofNullable(produto);
        } catch (HttpClientErrorException.NotFound notFound) {
            return Optional.empty();
        }
    }
}
