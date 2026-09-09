# TP2 — Do Docker ao Kubernetes

**Aluno:** Gustavo Cassel
**Disciplina:** Microsserviços e DevOps com Spring Boot e Spring Cloud
**Tema escolhido:** Loja virtual

> Este projeto foi construído e **validado de ponta a ponta**: os dois
> microsserviços foram compilados, testados (`mvn test`), containerizados,
> subidos com Docker Compose e migrados para um cluster Kubernetes real
> (`kind`), com evidências reais capturadas em cada etapa — não são só
> comandos sugeridos, é o que de fato rodou. A seção 14 (reflexão pessoal)
> ficou só com a estrutura: é sua experiência, não dá para eu escrever por
> você.

---

## 1. Planejamento da aplicação

- **Nome do projeto:** TP2 — Do Docker ao Kubernetes
- **Objetivo:** demonstrar a evolução de uma aplicação de microsserviços desde
  a execução manual em containers Docker até a orquestração completa no
  Kubernetes, passando por comunicação entre containers, rede dedicada,
  Docker Compose, Deployments/Services e balanceamento de carga por réplicas.
- **Microsserviços:**

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `product-service` | 8081 | Catálogo de produtos (CRUD simples, dados em memória) |
| `order-service` | 8082 | Ciclo de vida dos pedidos; ao criar um pedido, consulta o `product-service` para confirmar que o produto existe |

Estrutura de pastas:

```
gustavo_cassel_dr3_tp2/
├── README.md
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

## 2. Máquina virtual × Container

| Característica | Máquina Virtual | Container |
|---|---|---|
| Sistema operacional | Cada VM roda um SO convidado completo (kernel próprio) sobre um hypervisor | Todos os containers compartilham o kernel do SO hospedeiro |
| Consumo de recursos | Alto — reserva CPU/RAM/disco fixos para o SO inteiro | Baixo — só o processo da aplicação, sem duplicar o SO |
| Inicialização | Lenta (segundos a minutos, precisa dar boot num SO completo) | Rápida (o `product-service` e o `order-service` sobem em ~1 segundo cada, como visto nos logs do `docker compose up`) |
| Isolamento | Forte, em nível de hardware virtualizado | Em nível de processo/kernel (namespaces e cgroups) — mais leve, um pouco menos forte |

a) **Com máquina virtual:** instalar um hypervisor (VirtualBox/VMware) no
computador de destino, criar uma VM com um SO completo, instalar o JDK dentro
dela e rodar os dois `.jar` manualmente — cada serviço competindo por
recursos de uma VM inteira, mesmo sendo aplicações pequenas.

b) **Com containers:** instalar o Docker no computador de destino e rodar
`docker compose up -d --build` — as duas imagens (`tp2/product-service` e
`tp2/order-service`) sobem isoladas, sem precisar instalar nenhum SO
adicional além do próprio Docker.

c) **Containers** consomem bem menos recursos: no teste real deste projeto,
os dois serviços (mais o próprio Docker Engine) rodaram tranquilamente na
mesma máquina, cada container usando só a memória do seu processo Java —
nenhuma VM foi necessária.

d) **Por que containers favorecem a arquitetura de microsserviços** (aplicado
a este projeto):
- **Isolamento:** o `product-service` e o `order-service` rodam cada um no
  seu próprio container, com seu próprio processo e sistema de arquivos —
  uma falha ou um travamento em um não derruba o outro (comprovado na seção
  13, quando um Pod do `product-service` foi apagado e o `order-service`
  continuou funcionando normalmente).
- **Portabilidade:** a mesma imagem (`tp2/product-service:1.0.0`) rodou sem
  nenhuma alteração tanto via `docker run`/`docker compose` quanto dentro dos
  Pods do Kubernetes — o artefato é o mesmo, só muda quem o executa.
- **Escalabilidade:** para ter 3 instâncias do `product-service` respondendo
  em paralelo (seção 13), bastou declarar `replicas: 3` no Deployment — não
  foi preciso reinstalar nada nem duplicar um SO inteiro.
- **Independência de deploy:** os dois microsserviços têm Dockerfiles e
  imagens próprias — dá para reconstruir e reiniciar o `order-service` sem
  tocar no `product-service`, e vice-versa.

---

## 3-4. Os dois microsserviços

- `product-service`: `GET /products`, `GET /products/{id}`, `POST /products`
  (código em [`product-service/src/main/java`](product-service/src/main/java/br/edu/tp2/productservice)).
- `order-service`: `GET /orders`, `POST /orders`, `GET /orders/{id}`
  (código em [`order-service/src/main/java`](order-service/src/main/java/br/edu/tp2/orderservice)).

**Validação real:** cada serviço tem testes automatizados
(`ProductControllerTest`, `OrderControllerTest`) rodados com `mvn test` —
12 testes, todos passando, cobrindo os 6 endpoints e os casos de sucesso e
erro (404, 400):

```
$ cd product-service && mvn test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

$ cd order-service && mvn test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Rodando localmente, sem Docker (dois terminais):

```bash
cd product-service && mvn spring-boot:run
cd order-service   && mvn spring-boot:run
```

---

## 5. Criando as imagens Docker

```bash
docker build -t tp2/product-service:1.0.0 ./product-service
docker build -t tp2/order-service:1.0.0 ./order-service
```

**Evidência real (build executado e validado neste projeto):** as duas
imagens foram construídas com sucesso (`docker build` com `BUILD SUCCESS` do
Maven dentro do multi-stage build) e os dois containers rodaram e
responderam aos endpoints — ver evidência completa na seção 7/9 abaixo, com
`docker compose ps` mostrando os dois `(healthy)`.

---

## 6. Entendendo os componentes Docker

- **Dockerfile:** a receita de como construir a imagem — ver
  [`product-service/Dockerfile`](product-service/Dockerfile). Define a
  imagem base (`maven:3.9.9-eclipse-temurin-17` para build,
  `eclipse-temurin:17-jre-alpine` para runtime), copia o código, compila e
  define o comando de entrada (`ENTRYPOINT ["java", "-jar", "app.jar"]`).
- **Image:** o "molde" gerado a partir do Dockerfile (ex.:
  `tp2/product-service:1.0.0`) — só leitura, versionável, reutilizável para
  criar quantos containers quiser. Neste projeto, a **mesma imagem**
  `tp2/product-service:1.0.0` foi usada para criar 3 Pods diferentes no
  Kubernetes (seção 13).
- **Container:** uma instância em execução dessa imagem, com processo
  isolado e sistema de arquivos próprio — ex.: o container `product-service`
  rodando a partir da imagem `tp2/product-service:1.0.0`, visto com
  `docker ps`:
  ```
  NAMES             IMAGE                       STATUS
  order-service     tp2/order-service:1.0.0     Up (healthy)
  product-service   tp2/product-service:1.0.0   Up (healthy)
  ```
- **Docker Engine:** o daemon (`dockerd`) que efetivamente cria, executa e
  gerencia containers, imagens, redes e volumes na máquina — é ele quem
  respondeu a cada `docker build`/`docker run`/`docker compose` executado
  neste projeto.
- **Port Mapping:** a associação entre uma porta do host e uma porta do
  container — ex. `"8081:8081"` no `docker-compose.yml` — necessária porque
  o container tem sua própria pilha de rede isolada. É por isso que
  `curl http://localhost:8081/products` (do host) chega até o processo Java
  rodando dentro do container.
- **Network:** uma rede virtual do Docker (`tp2-network`, criada pelo
  `docker-compose.yml`) que conecta os containers entre si e dá a cada um
  resolução de nome (DNS) pelo nome do container — ver seção 7/8.

**Diferença entre imagem e container:** a imagem é estática e imutável (o
molde); o container é a instância em execução dessa imagem, com estado
próprio. Dá para rodar N containers a partir da mesma imagem — é exatamente
o que este projeto demonstra: os 3 Pods do `product-service` (seção 13)
foram todos criados a partir da mesma imagem `tp2/product-service:1.0.0`,
mas são 3 containers/processos distintos (hostnames diferentes:
`product-service-678ccd8d6-jckv4`, `-mknzj`, `-p4kvg`).

---

## 7. Comunicação entre microsserviços (sem localhost)

O `order-service` consulta o `product-service` via
[`ProductClient`](order-service/src/main/java/br/edu/tp2/orderservice/client/ProductClient.java)
antes de confirmar um pedido. O endereço vem da variável de ambiente
`PRODUCT_SERVICE_URL` — em Docker/Kubernetes ela aponta para o **nome do
container/Service** (`http://product-service:8081`), nunca para `localhost`.

**Evidência real** (containers rodando em hosts/containers diferentes,
comunicação via nome, sem `localhost`):

```
$ docker exec order-service printenv PRODUCT_SERVICE_URL
http://product-service:8081

$ curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}'
{"id":1,"productId":1,"quantity":2,"status":"CONFIRMADO"}   # HTTP 201

$ curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d '{"productId":999,"quantity":1}'
{"productId":999,"erro":"Produto nao encontrado no product-service"}   # HTTP 400
```

O segundo teste (produto 999, que não existe) prova que o `order-service`
de fato **consultou** o `product-service` antes de decidir — se fosse só
uma simulação local, teria confirmado o pedido de qualquer forma.

**Como os containers se encontraram:** os dois estão na mesma rede Docker
definida pelo usuário (`tp2-network`, criada pelo `docker-compose.yml`). O
Docker fornece um servidor DNS interno para redes desse tipo, que resolve o
**nome do container** (`product-service`) para o IP interno correto
automaticamente — por isso o `order-service` nunca precisa saber (nem usar)
`localhost` ou um IP fixo.

---

## 8. Docker Network

**Comando utilizado para criar a rede** (o `docker-compose.yml` cria
automaticamente ao rodar `docker compose up`; o comando equivalente manual
é):

```bash
docker network create tp2-network
```

**Containers conectados** (evidência real, `docker network inspect`):

```
$ docker network inspect tp2-network --format '{{range .Containers}}{{.Name}} -> {{.IPv4Address}}{{"\n"}}{{end}}'
product-service -> 172.18.0.2/16
order-service -> 172.18.0.3/16
```

**Evidência da comunicação:** o `POST /orders` com produto existente
retornando `201 Created` com `"status":"CONFIRMADO"` (seção 7) — só é
possível porque o `order-service` alcançou o `product-service` pela rede.

---

## 9. Docker Compose

Arquivo: [`docker-compose.yml`](docker-compose.yml) — configura os dois
serviços, portas, a rede `tp2-network` e a variável `PRODUCT_SERVICE_URL`.

**Desafio (remover os containers manuais e subir só com o Compose) —
executado e validado:**

```bash
docker compose up -d
docker compose ps
```

```
NAME              IMAGE                       STATUS                    PORTS
order-service     tp2/order-service:1.0.0     Up (healthy)              0.0.0.0:8082->8082/tcp
product-service   tp2/product-service:1.0.0   Up (healthy)              0.0.0.0:8081->8081/tcp
```

Os dois containers subiram, ficaram `healthy` (healthcheck do Actuator) e
responderam a todos os testes da seção 7 — todo o ecossistema rodando
somente a partir do `docker-compose.yml`, sem nenhum `docker run` manual.

---

## 10. Preparando a migração para Kubernetes

| Docker | Kubernetes |
|---|---|
| **Container** | **Pod** (encapsula um ou mais containers) |
| **Network** | **Service** (+ Namespace) — rede interna do cluster, com DNS por nome de Service |
| Port Mapping | `containerPort` no Pod + `port`/`targetPort`/`nodePort` no Service |
| Serviço do `docker-compose.yml` | **Deployment** (gerencia os Pods; substitui o papel do `docker compose`) |
| Múltiplos containers (`docker compose --scale`) | `replicas` no Deployment |

**Por que a aplicação não precisa ser reescrita:** o Kubernetes não
substitui o Docker — ele orquestra os mesmos containers. A **mesma imagem**
Docker construída na seção 5 (`tp2/product-service:1.0.0`) foi usada sem
nenhuma alteração dentro do Pod no Kubernetes (comprovado na seção 11: o
Deployment referencia exatamente essa imagem). Só muda a camada de
infraestrutura que decide onde e quantas vezes essa imagem roda, quem
reinicia o container se ele cair e como o tráfego é distribuído — o código
da aplicação e a imagem continuam os mesmos.

---

## 11-13. Kubernetes: Deployment, Service, réplicas e balanceamento

Manifests em [`k8s/`](k8s/):

- [`0-namespace.yaml`](k8s/0-namespace.yaml) — cria o Namespace `tp2-loja`.
- [`1-product-service.yaml`](k8s/1-product-service.yaml) — Deployment com
  **3 réplicas** + Service (`NodePort 30081`).
- [`2-order-service.yaml`](k8s/2-order-service.yaml) — Deployment com 1
  réplica + Service (`NodePort 30082`), apontando para
  `http://product-service:8081` (nome do **Service**, nunca IP fixo).

### Comandos utilizados (cluster local via `kind`)

```bash
kind create cluster --name tp2 --config kind-config.yaml

docker build -t tp2/product-service:1.0.0 ./product-service
docker build -t tp2/order-service:1.0.0 ./order-service
kind load docker-image tp2/product-service:1.0.0 --name tp2
kind load docker-image tp2/order-service:1.0.0 --name tp2

kubectl apply -f k8s/
kubectl get pods -n tp2-loja -w
```

### Evidência real — Pods e Services no ar

```
$ kubectl get all -n tp2-loja
NAME                                  READY   STATUS    RESTARTS   AGE
pod/order-service-79959c4fd9-rx5ch    1/1     Running   0          27s
pod/product-service-678ccd8d6-jckv4   1/1     Running   0          27s
pod/product-service-678ccd8d6-mknzj   1/1     Running   0          27s
pod/product-service-678ccd8d6-p4kvg   1/1     Running   0          27s

NAME                      TYPE       CLUSTER-IP     PORT(S)          SELECTOR
service/order-service     NodePort   10.96.85.95    8082:30082/TCP   app=order-service
service/product-service   NodePort   10.96.93.126   8081:30081/TCP   app=product-service

NAME                              READY   UP-TO-DATE   AVAILABLE
deployment.apps/order-service     1/1     1            1
deployment.apps/product-service   3/3     3            3
```

### Evidência real — comunicação via nome do Service (seção 12)

```
$ kubectl exec -n tp2-loja order-service-79959c4fd9-rx5ch -- printenv PRODUCT_SERVICE_URL
http://product-service:8081

$ curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d '{"productId":1,"quantity":3}'
{"id":1,"productId":1,"quantity":3,"status":"CONFIRMADO"}   # HTTP 201
```

O `order-service`, rodando dentro de um Pod no Kubernetes, encontrou e
consultou o `product-service` usando somente o **nome do Service**
(`product-service`) — nenhum IP fixo em lugar nenhum da configuração.

### Evidência real — balanceamento de carga entre os 3 Pods (seção 13)

O endpoint auxiliar `GET /products/instance` devolve o hostname do Pod que
respondeu. Chamando o mesmo endereço do Service 10 vezes seguidas:

```
$ for i in $(seq 1 10); do curl -s http://localhost:8081/products/instance; echo; done
{"hostname":"product-service-678ccd8d6-p4kvg"}
{"hostname":"product-service-678ccd8d6-mknzj"}
{"hostname":"product-service-678ccd8d6-jckv4"}
{"hostname":"product-service-678ccd8d6-jckv4"}
{"hostname":"product-service-678ccd8d6-mknzj"}
{"hostname":"product-service-678ccd8d6-mknzj"}
{"hostname":"product-service-678ccd8d6-mknzj"}
{"hostname":"product-service-678ccd8d6-p4kvg"}
{"hostname":"product-service-678ccd8d6-p4kvg"}
{"hostname":"product-service-678ccd8d6-mknzj"}
```

Três hostnames diferentes responderam à mesma chamada — prova de que o
Service distribuiu as requisições entre os 3 Pods.

### Evidência real — o que acontece quando um Pod cai

```
$ kubectl delete pod product-service-678ccd8d6-jckv4 -n tp2-loja
pod "product-service-678ccd8d6-jckv4" deleted

$ curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:8081/products
[...]
HTTP_STATUS:200        # o Service continuou respondendo, via os outros 2 Pods

$ kubectl get pods -n tp2-loja -l app=product-service
NAME                              READY   STATUS    AGE
product-service-678ccd8d6-mknzj   1/1     Running   79s
product-service-678ccd8d6-p4kvg   1/1     Running   79s
product-service-678ccd8d6-zq5db   0/1     Running   18s   <- pod novo, criado sozinho

# ~15s depois:
NAME                              READY   STATUS    AGE
product-service-678ccd8d6-mknzj   1/1     Running   101s
product-service-678ccd8d6-p4kvg   1/1     Running   101s
product-service-678ccd8d6-zq5db   1/1     Running   40s   <- pronto e recebendo trafego
```

O Deployment detectou a falta do Pod deletado e criou automaticamente
`product-service-678ccd8d6-zq5db` no lugar, sem nenhuma intervenção manual
— e o Service nunca ficou fora do ar, porque os outros 2 Pods continuaram
respondendo durante a substituição.

### Respostas (seção 13)

a) **Função do Service:** dar um nome de rede fixo e estável a um conjunto
de Pods, escondendo quantos Pods existem e qual deles atende cada
requisição.

b) **Por que vários Pods do mesmo microsserviço:** para distribuir carga
(mais requisições atendidas em paralelo) e para tolerância a falhas — se um
cair, os outros continuam respondendo (comprovado acima).

c) **Se um Pod parar de funcionar:** o Deployment detecta e cria
automaticamente outro Pod no lugar, para manter o número de réplicas
definido (3); o Service para de enviar tráfego para o Pod que caiu assim que
ele fica `NotReady`, e o novo Pod só entra na rotação quando fica `Ready`
(comprovado acima: o Service respondeu `200` durante toda a substituição).

d) **Como o Service distribui as requisições:** ele mantém a lista de Pods
`Ready` que casam com o `selector` (`app: product-service`) e distribui
entre eles a cada nova requisição/conexão — evidenciado pelos hostnames
diferentes retornados nas 10 chamadas consecutivas.

---

## 14. Avaliação e reflexão sobre o projeto

> Preencher com suas próprias palavras, a partir da sua experiência rodando
> o projeto (comandos do runbook acima).

a) **Parte mais fácil / mais difícil:** _(a escrever)_

b) **Principal aprendizado sobre Docker, containers e Kubernetes:**
_(a escrever)_

c) **Nota de 0 a 10 e justificativa:** _(a escrever)_

---

## Como reproduzir tudo do zero

```bash
# 1. Compilar e testar
cd product-service && mvn clean test && cd ..
cd order-service   && mvn clean test && cd ..

# 2. Docker Compose
docker compose up -d --build
docker compose ps
curl http://localhost:8081/products
curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}'
docker compose down

# 3. Kubernetes
kind create cluster --name tp2 --config kind-config.yaml
kind load docker-image tp2/product-service:1.0.0 --name tp2
kind load docker-image tp2/order-service:1.0.0 --name tp2
kubectl apply -f k8s/
kubectl get pods -n tp2-loja -w

curl http://localhost:8081/products
curl -X POST http://localhost:8082/orders -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}'
for i in $(seq 1 10); do curl -s http://localhost:8081/products/instance; echo; done

# 4. Encerrar
kind delete cluster --name tp2
```
