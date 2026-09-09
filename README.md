# TP2 - Do Docker ao Kubernetes

**Aluno:** Gustavo Cassel
**Disciplina:** Microsserviços e DevOps com Spring Boot e Spring Cloud
**Tema escolhido:** Loja virtual

> A resposta completa do trabalho (as 14 seções do enunciado, tabelas,
> explicações e evidências) está no arquivo
> [`gustavo_cassel_DR3_TP2.docx`](gustavo_cassel_DR3_TP2.docx). Este README é
> só um guia rápido do projeto e de como rodá-lo.

---

## O projeto

Dois microsserviços Spring Boot que evoluem de containers Docker soltos até
um deploy completo no Kubernetes (`kind`), com rede dedicada, Docker Compose,
Service Discovery e balanceamento de carga via réplicas.

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `product-service` | 8081 | Catálogo de produtos (CRUD simples, dados em memória) |
| `order-service` | 8082 | Ciclo de vida dos pedidos; ao criar um pedido, consulta o `product-service` para confirmar que o produto existe |

### Endpoints

- `product-service`: `GET /products`, `GET /products/{id}`, `POST /products`, `GET /products/instance` (retorna o hostname do Pod/container que respondeu)
- `order-service`: `GET /orders`, `GET /orders/{id}`, `POST /orders`

### Estrutura de pastas

```
gustavo_cassel_dr3_tp2/
├── README.md
├── gustavo_cassel_DR3_TP2.docx   (respostas completas do TP2)
├── docker-compose.yml
├── kind-config.yaml
├── k8s/
│   ├── 0-namespace.yaml
│   ├── 1-product-service.yaml   (Deployment + Service, 3 réplicas)
│   └── 2-order-service.yaml     (Deployment + Service, 1 réplica)
├── product-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/br/edu/tp2/productservice/...
└── order-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/br/edu/tp2/orderservice/...
```

---

## Como rodar

### 1. Compilar e testar

```bash
cd product-service && mvn clean test && cd ..
cd order-service   && mvn clean test && cd ..
```

### 2. Docker Compose

```bash
docker compose up -d --build
docker compose ps

curl http://localhost:8081/products
curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\":1,\"quantity\":2}"

docker compose down
```

### 3. Kubernetes (via `kind`)

```bash
kind create cluster --name tp2 --config kind-config.yaml

docker build -t tp2/product-service:1.0.0 ./product-service
docker build -t tp2/order-service:1.0.0 ./order-service
kind load docker-image tp2/product-service:1.0.0 --name tp2
kind load docker-image tp2/order-service:1.0.0 --name tp2

kubectl apply -f k8s/
kubectl get pods -n tp2-loja -w
```

```bash
curl http://localhost:8081/products
curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d "{\"productId\":1,\"quantity\":2}"

# balanceamento entre os 3 Pods do product-service (cmd.exe):
for /L %i in (1,1,10) do curl -s http://localhost:8081/products/instance & echo.
```

### 4. Encerrar

```bash
docker compose down
kind delete cluster --name tp2
```

---

**Nota sobre `localhost` em requisições:** dentro dos containers/Pods, o
`order-service` nunca usa `localhost` para falar com o `product-service` - ele
usa o nome do container (Docker) ou o nome do Service (Kubernetes), via a
variável de ambiente `PRODUCT_SERVICE_URL`. O `localhost` só aparece nos
comandos `curl` acima porque estes são executados do **host** (sua máquina),
que expõe as portas via Port Mapping (Docker) / `extraPortMappings` (kind).
