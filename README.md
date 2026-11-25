# HealthCare - Implantação em Kubernetes e Observabilidade

Sistema web de triagem hospitalar (**HealthCare**) desenvolvido em Java 17 (Spring Boot) com banco de dados PostgreSQL 16, implantado em cluster Kubernetes (Minikube) com stack completa de observabilidade (Métricas, Logs e Traces).

## Arquitetura da Solução

* **Aplicação (`healthcare-app`):** Deployment com 2 réplicas na porta `8088`, instrumentado com OpenTelemetry Java Agent.
* **Banco de Dados (`postgres`):** Deployment PostgreSQL 16 (`healthcare-app`) na porta `5432`, isolado via `ClusterIP`.
* **Exposição (`app-service`):** Service `LoadBalancer` / `NodePort` expondo o front-end e API REST.
* **Observabilidade (`monitoring`):**
  * **OpenTelemetry Collector:** Recebe telemetria OTLP (`4317`/`4318`) e exporta traces para o Tempo.
  * **Prometheus:** Coleta de métricas de infraestrutura, pods e JVM.
  * **Loki & Promtail:** Agregação e consulta centralizada de logs dos contêineres.
  * **Tempo:** Rastreamento distribuído (*Distributed Tracing*) de requisições HTTP e consultas SQL.
  * **Grafana:** Visualização unificada de métricas, logs e traces.

## Estrutura do Repositório

```text
├── k8s/
│   ├── app-deployment.yaml
│   ├── app-service.yaml
│   ├── banco-deployment.yaml
│   ├── banco-service.yaml
│   └── observabilidade/
│       └── otel-collector.yaml
├── evidencias/
│   ├── 01-aplicacao-navegador.png
│   ├── 02-kubectl-get-pods.png
│   ├── 03-grafana-prometheus.png
│   ├── 04-grafana-loki.png
│   └── 05-grafana-tempo.png
├── Dockerfile
└── README.md


## Testes Automatizados (Avaliação N2 - Teste de Software)

Os testes automatizados foram implementados em Java 17 utilizando **JUnit 5**, **Mockito** e **Spring MockMvc** na classe `HealthCare/codigo/src/test/java/br/senai/healthcare/AvaliacaoN2Test.java`.

### Pré-requisitos
* **Java JDK 17+**
* **Apache Maven 3.8+** (ou Maven Wrapper)

### Como executar os testes
1. Acesse a pasta do código-fonte Java:
   ```bash
   cd HealthCare/codigo