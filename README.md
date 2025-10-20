# 🐝 TechMel - Backend

## Sobre o Projeto

TechMel é uma startup apoiada pelo SEBRAE (Startup Nordeste) que revoluciona a apicultura através de tecnologia IoT. O sistema monitora colmeias em tempo real usando sensores que capturam temperatura, umidade e CO₂, fornecendo insights para apicultores tomarem decisões mais eficientes e sustentáveis.

## 🚀 Tecnologias

- **Java 17+** com **Spring Boot 3.x**
- **PostgreSQL** (hospedado no Neon)
- **Redis** (cache e rate limiting)
- **JWT** + **OAuth2** (Google)
- **Docker** + **Docker Compose**

## 📦 Principais Dependências

- Spring Data JPA
- Spring Security
- Spring Mail
- Spring Validation
- Thymeleaf (templates de email)
- Springdoc OpenAPI (documentação)
- Mercado Pago SDK (gateway de pagamento)

## 🏗️ Arquitetura

O projeto segue princípios **SOLID** e **Clean Architecture**, organizado em camadas:

- **Domain**: Entidades e lógica de negócio
- **Application**: Casos de uso e serviços
- **Infrastructure**: Adaptadores (API, Persistência, Cache, Email)

## 💳 Integração de Pagamentos

Sistema de pagamentos integrado com **Mercado Pago** para:
- Assinaturas e planos premium
- Processamento seguro de transações
- Webhooks para atualização automática de status

## 🔧 Configuração

1. Clone o repositório
2. Copie `.env.example` para `.env` e configure as variáveis
3. Execute com Docker:
```bash
docker-compose up
```

Ou localmente:
```bash
./mvnw spring-boot:run
```

## 📝 Documentação da API

Acesse a documentação Swagger em: `http://localhost:8080/swagger-ui.html`

---
