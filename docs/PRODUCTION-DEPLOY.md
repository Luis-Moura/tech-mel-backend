# Guia de Deploy para Produção - TechMel Backend

## Sumário

1. Visão Geral do Deploy
2. Preparação para Produção
3. Configuração do Docker Hub
4. Build e Push da Imagem
5. Configuração no Render
6. Variáveis de Ambiente de Produção
7. Configuração de Banco de Dados
8. Monitoramento e Logs
9. CI/CD e Automação
10. Troubleshooting de Produção

## 1. Visão Geral do Deploy

### Arquitetura de Produção

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Render        │    │   Docker Hub    │    │   Neon          │
│   Web Service   │◄──►│   Registry      │    │   PostgreSQL    │
│                 │    │                 │    │                 │
│ • Aplicação     │    │ • Imagem Docker │    │ • Banco Managed │
│ • Load Balancer │    │ • Auto Deploy   │    │ • Backups Auto  │
│ • SSL/HTTPS     │    │ • Versionamento │    │ • Monitoramento │
│ • Health Checks │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                               ▼
                      ┌─────────────────┐
                      │   Redis Cloud   │
                      │   (Upstash)     │
                      └─────────────────┘
```

### Stack de Produção

- **Aplicação**: Render Web Service
- **Banco de Dados**: Neon PostgreSQL (Managed)
- **Cache**: Upstash Redis ou Render Redis
- **Container Registry**: Docker Hub
- **SSL**: Automático via Render
- **Monitoramento**: Render Dashboard + Logs

## 2. Preparação para Produção

### 🔧 Ajustes no Código

#### Otimização do Dockerfile

Verifique se o `Dockerfile` está otimizado para produção:

```dockerfile
# Dockerfile otimizado para produção
FROM maven:3.8.1-openjdk-17-slim AS builder
WORKDIR /app

# Copiar apenas pom.xml primeiro (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Build da aplicação
RUN mvn clean package -DskipTests -Dspring.profiles.active=prod

# Etapa final - imagem enxuta
FROM openjdk:17-jdk-alpine
WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001

# Copiar JAR
COPY --from=builder /app/target/*.jar app.jar

# Mudar para usuário não-root
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Configuração de Produção

Crie/ajuste `src/main/resources/application-prod.properties`:

```properties
# ===========================
# CONFIGURACAO DE PRODUCAO
# ===========================

spring.application.name=tech-mel

# URLs
app.url=${APP_URL}
app.url.frontend=${APP_URL_FRONTEND}

# ===========================
# BANCO DE DADOS
# ===========================
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# PRODUCAO: DDL desabilitado
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Pool de conexoes
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000

# ===========================
# REDIS
# ===========================
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.password=${REDIS_PASSWORD}
spring.data.redis.timeout=2000ms
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2

# ===========================
# JWT
# ===========================
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}
jwt.refresh-expiration=${JWT_REFRESH_EXPIRATION}

# ===========================
# OAUTH2
# ===========================
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.redirect-uri=${REDIRECT_URI}
app.oauth2.redirect-uri=${FRONTEND_CALLBACK_URI}

# ===========================
# EMAIL
# ===========================
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.from=${MAIL_FROM}

# ===========================
# LOGGING
# ===========================
logging.level.com.tech_mel.tech_mel=INFO
logging.level.org.springframework.security=WARN
logging.level.org.hibernate.SQL=WARN
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# ===========================
# ACTUATOR (Monitoramento)
# ===========================
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.metrics.export.prometheus.enabled=true

# ===========================
# SEGURANCA
# ===========================
server.error.include-stacktrace=never
server.error.include-message=never
```

### 🔒 Checklist de Segurança

- [ ] JWT_SECRET com 64+ caracteres aleatórios
- [ ] Senhas do banco geradas automaticamente
- [ ] CORS configurado apenas para domínios de produção
- [ ] Logs não expõem dados sensíveis
- [ ] Health checks configurados
- [ ] SSL/HTTPS obrigatório

## 3. Configuração do Docker Hub

### Criando Conta no Docker Hub

1. Acesse [hub.docker.com](https://hub.docker.com)
2. Crie uma conta gratuita
3. Crie um repositório público: `seu-usuario/tech-mel`
4. Anote as credenciais para CI/CD

### Configuração Local

```bash
# Login no Docker Hub
docker login

# Verificar login
docker info | grep Username
```

### Criação do Repositório

```bash
# Opção 1: Via interface web (recomendado)
# 1. Acesse hub.docker.com
# 2. Click "Create Repository"
# 3. Nome: tech-mel
# 4. Visibilidade: Public
# 5. Descrição: "TechMel Backend API"

# Opção 2: Via linha de comando
docker tag tech-mel:latest seu-usuario/tech-mel:latest
docker push seu-usuario/tech-mel:latest
```

## 4. Build e Push da Imagem

### Script de Build Automatizado

Crie um arquivo `scripts/build-production.sh`:

```bash
#!/bin/bash

# Script de build para produção
set -e

# Variáveis
DOCKER_USER="seu-usuario"
APP_NAME="tech-mel"
VERSION=$(date +%Y%m%d-%H%M%S)
LATEST_TAG="latest"

echo "🚀 Iniciando build de produção..."

# 1. Limpar builds anteriores
echo "🧹 Limpando builds anteriores..."
docker system prune -f

# 2. Build da imagem com tag de versão
echo "🔨 Building imagem Docker..."
docker build -t $DOCKER_USER/$APP_NAME:$VERSION .
docker build -t $DOCKER_USER/$APP_NAME:$LATEST_TAG .

# 3. Push para Docker Hub
echo "📤 Enviando para Docker Hub..."
docker push $DOCKER_USER/$APP_NAME:$VERSION
docker push $DOCKER_USER/$APP_NAME:$LATEST_TAG

echo "✅ Build concluído!"
echo "📦 Imagem: $DOCKER_USER/$APP_NAME:$VERSION"
echo "📦 Latest: $DOCKER_USER/$APP_NAME:$LATEST_TAG"
echo ""
echo "🚀 Para deploy no Render, use:"
echo "   $DOCKER_USER/$APP_NAME:$LATEST_TAG"
```

### Executando o Build

```bash
# Dar permissão de execução
chmod +x scripts/build-production.sh

# Executar build
./scripts/build-production.sh
```

### Build Manual

```bash
# 1. Build da imagem
docker build -t seu-usuario/tech-mel:latest .

# 2. Testar localmente (opcional)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="postgresql://..." \
  seu-usuario/tech-mel:latest

# 3. Push para Docker Hub
docker push seu-usuario/tech-mel:latest
```

### Verificação da Imagem

```bash
# Verificar se a imagem foi enviada
docker search seu-usuario/tech-mel

# Ou verificar no navegador
# https://hub.docker.com/r/seu-usuario/tech-mel
```

## 5. Configuração no Render

### 🆕 Criando Web Service

1. **Acesse** [render.com](https://render.com)
2. **Faça login** ou crie uma conta
3. **Click** "New +" → "Web Service"
4. **Selecione** "Deploy an existing image from a registry"
5. **Configure:**
   - **Image URL**: `seu-usuario/tech-mel:latest`
   - **Name**: `techmel-backend`
   - **Region**: `Oregon (US West)` ou mais próximo
   - **Branch**: `main` (se conectar ao Git)

### ⚙️ Configurações do Serviço

#### Plan & Pricing
- **Instance Type**: `Starter` (gratuito) ou `Basic` ($7/mês)
- **Auto-Deploy**: `Yes` (recomendado)

#### Advanced Settings
```
Build Command: (deixar vazio - usa Docker)
Start Command: (deixar vazio - usa ENTRYPOINT do Docker)
Port: 8080
Environment: Production
```

## 6. Variáveis de Ambiente de Produção

### 🔐 Configuração no Render

No painel do Render, vá em **Environment** e configure:

```bash
# ===========================
# CONFIGURAÇÕES BÁSICAS
# ===========================
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# URLs da aplicação
APP_URL=https://seu-app.onrender.com
APP_URL_FRONTEND=https://seu-frontend.vercel.app

# ===========================
# BANCO DE DADOS
# ===========================
# Será preenchido automaticamente pelo Render PostgreSQL
DATABASE_URL=postgresql://user:password@host:port/database

# ===========================
# REDIS
# ===========================
# Opção 1: Upstash Redis (recomendado)
REDIS_URL=redis://default:password@host:port

# Opção 2: Render Redis
# REDIS_URL será preenchido automaticamente

# ===========================
# JWT (GERAR NOVAS CHAVES!)
# ===========================
JWT_SECRET=sua-chave-super-secreta-de-64-caracteres-ou-mais-para-producao
JWT_EXPIRATION=1800000
JWT_REFRESH_EXPIRATION=2592000000

# ===========================
# OAUTH2 - GOOGLE
# ===========================
GOOGLE_CLIENT_ID=seu-client-id-google.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=seu-client-secret-google
GOOGLE_REDIRECT_URI=https://seu-app.onrender.com/login/oauth2/code/google
OAUTH2_FRONTEND_CALLBACK=https://seu-frontend.vercel.app/oauth/callback

# ===========================
# EMAIL (PRODUÇÃO)
# ===========================
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-app
MAIL_FROM=noreply@techmel.com
```

### 🔑 Gerando Chaves Seguras

```bash
# Gerar JWT_SECRET seguro
openssl rand -hex 64

# Ou online: https://generate-secret.vercel.app/64

# Exemplo de resultado:
# a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6a7b8c9d0e1f2
```

### 📧 Configuração do Email (Gmail)

1. **Ativar 2FA** na sua conta Google
2. **Gerar senha de app**:
   - Google Account → Security → 2-Step Verification
   - App passwords → Generate
   - Use a senha gerada em `MAIL_PASSWORD`

## 7. Configuração de Banco de Dados

### 🗄️ PostgreSQL no Render

#### Criando o Banco

1. **No Dashboard** do Render → "New +" → "PostgreSQL"
2. **Configure:**
   - **Name**: `techmel-database`
   - **Database**: `techmel_db`
   - **User**: `techmel_user`
   - **Region**: Mesma região do Web Service
   - **PostgreSQL Version**: 15
   - **Plan**: Free ou Starter ($7/mês)

#### Conectando ao Web Service

1. **No Web Service** → "Environment"
2. **Adicionar** variável:
   ```
   DATABASE_URL = ${{techmel-database.DATABASE_URL}}
   ```
3. O Render automaticamente substitui pela URL real

### 🔄 Migrações de Banco

#### Opção 1: Flyway (Recomendado)

Adicione ao `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

Crie arquivos em `src/main/resources/db/migration/`:

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'COMMON',
    available_hives INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V2__create_hives_table.sql
CREATE TABLE hives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    location TEXT NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    hive_status VARCHAR(50) NOT NULL DEFAULT 'INACTIVE',
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hives_owner_id ON hives(owner_id);
```

#### Opção 2: JPA com DDL (Temporário)

Para deploy inicial rápido:

```properties
# application-prod.properties (temporário)
spring.jpa.hibernate.ddl-auto=create-drop  # APENAS PRIMEIRA VEZ
# Depois mudar para:
# spring.jpa.hibernate.ddl-auto=validate
```

### 🛠️ Acesso Direto ao Banco

```bash
# Conectar via psql (instalar PostgreSQL client localmente)
psql DATABASE_URL_DO_RENDER

# Ou via pgAdmin usando os dados de conexão do Render
```

## 8. Monitoramento e Logs

### 📊 Dashboard do Render

- **Metrics**: CPU, Memory, Response Time
- **Logs**: Real-time application logs
- **Health**: Automatic health checks
- **SSL**: Certificate management

### 🔍 Configuração de Logs

```properties
# application-prod.properties
logging.level.com.tech_mel=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=/tmp/techmel.log
```

## 9. Troubleshooting de Produção

### 🚨 Problemas Comuns

#### 1. "Application failed to start"

```bash
# Verificar logs no Render Dashboard
# Ou via CLI
render logs -s seu-service-id

# Causas comuns:
# - Variáveis de ambiente faltando
# - Porta errada (deve ser 8080)
# - Erro de conexão com banco
```

#### 2. "Database connection failed"

```bash
# Verificar variável DATABASE_URL
echo $DATABASE_URL

# Testar conexão manual
psql $DATABASE_URL -c "SELECT 1;"

# Verificar se PostgreSQL service está rodando
```

#### 3. "Memory limit exceeded"

```bash
# Otimizar JVM para Render
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Adicionar ao Dockerfile
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### 4. "Health check failing"

```bash
# Verificar endpoint de health
curl https://seu-app.onrender.com/actuator/health

# Verificar se porta 8080 está exposta
# Verificar se Spring Boot Actuator está configurado
```

### 🔧 Comandos de Debug

```bash
# Conectar ao container em produção (se possível)
docker exec -it container-id sh

# Verificar variáveis de ambiente
docker exec container-id printenv

# Verificar logs em tempo real
render logs -f -s seu-service-id

# Verificar métricas
curl https://seu-app.onrender.com/actuator/metrics
```

### 📞 Suporte e Rollback

#### Rollback Rápido

```bash
# 1. Voltar para versão anterior no Docker Hub
docker pull seu-usuario/tech-mel:versao-anterior
docker tag seu-usuario/tech-mel:versao-anterior seu-usuario/tech-mel:latest
docker push seu-usuario/tech-mel:latest

# 2. Trigger redeploy no Render
curl -X POST "https://api.render.com/deploy/srv-xxxxxxxxx?key=xxxxxxxxx"
```

#### Backup de Banco

```bash
# Fazer backup antes de deploys importantes
pg_dump $DATABASE_URL > backup-$(date +%Y%m%d).sql

# Restaurar backup
psql $DATABASE_URL < backup-20240623.sql
```

---

## 🎯 Checklist de Deploy

### Antes do Deploy
- [ ] Testes passando localmente
- [ ] Dockerfile otimizado
- [ ] Variáveis de ambiente configuradas
- [ ] JWT_SECRET gerado para produção
- [ ] Backup do banco atual
- [ ] OAuth2 configurado para domínio de produção

### Durante o Deploy
- [ ] Build da imagem Docker
- [ ] Push para Docker Hub
- [ ] Deploy no Render
- [ ] Verificar health checks
- [ ] Testar endpoints principais

### Após o Deploy
- [ ] Verificar logs por erros
- [ ] Testar autenticação
- [ ] Testar CRUD de hives
- [ ] Verificar performance
- [ ] Configurar monitoramento
- [ ] Documentar versão deployada

---

## 🔗 Links Úteis

- [Render Documentation](https://render.com/docs)
- [Docker Hub](https://hub.docker.com)
- [Upstash Redis](https://upstash.com)
- [Spring Boot Production Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [PostgreSQL Performance](https://www.postgresql.org/docs/current/performance-tips.html)
