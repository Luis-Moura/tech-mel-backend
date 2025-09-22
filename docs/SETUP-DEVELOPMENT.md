# Guia de Configuração e Execução - TechMel Backend

## Sumário

1. Pré-requisitos
2. Configuração do Ambiente
3. Configuração das Variáveis de Ambiente
4. Execução com Docker Compose
5. Execução Local (sem Docker)
6. Perfis de Aplicação
7. Configuração de Usuários de Teste
8. Testes das APIs
9. Monitoramento e Logs
10. Troubleshooting

## 1. Pré-requisitos

### Software Necessário

- **Docker**: versão 20.10 ou superior
- **Docker Compose**: versão 2.0 ou superior
- **Java 17**: (apenas para execução local)
- **Maven 3.8+**: (apenas para execução local)
- **Git**: para clonagem do repositório

### Verificação dos Pré-requisitos

```bash
# Verificar Docker
docker --version
docker-compose --version

# Verificar Java (se executar localmente)
java -version
mvn -version

# Verificar Git
git --version
```

### Portas Utilizadas

| Serviço | Porta | Descrição |
|---------|--------|-----------|
| Backend | 8080 | API REST principal |
| PostgreSQL | 5432 | Banco de dados |
| Redis | 6379 | Cache e sessões |

## 2. Configuração do Ambiente

### Estrutura do Projeto

```
tech-mel/
├── docs/                     # Documentação
├── src/                      # Código fonte
├── target/                   # Arquivos compilados
├── docker-compose.yml        # Configuração Docker
├── Dockerfile               # Imagem da aplicação
├── pom.xml                  # Dependências Maven
├── .env                     # Variáveis de ambiente
└── README.md
```

### Clonagem do Repositório

```bash
# Clonar o repositório
git clone <repository-url>
cd tech-mel

# Verificar estrutura
ls -la
```

## 3. Configuração das Variáveis de Ambiente

### Criando o Arquivo .env

Crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```bash
# ========================
# CONFIGURAÇÕES BÁSICAS
# ========================

# URLs da aplicação
APP_URL=http://localhost:8080
APP_URL_FRONTEND=http://localhost:3000

# ========================
# BANCO DE DADOS
# ========================

# PostgreSQL
DB_URL=jdbc:postgresql://tech-mel-postgres:5432/techmel_db
DB_USERNAME=techmel_user
DB_PASSWORD=techmel_password_2024

# ========================
# REDIS (Cache)
# ========================

REDIS_HOST=tech-mel-redis
REDIS_PORT=6379
REDIS_PASSWORD=redis_password_2024
REDIS_TIMEOUT=2000
REDIS_POOL_MAX_TOTAL=8
REDIS_POOL_MAX_IDLE=8
REDIS_POOL_MIN_IDLE=0

# ========================
# SEGURANÇA JWT
# ========================

# Chave secreta para JWT (TROCAR EM PRODUÇÃO!)
JWT_SECRET=b123e9e19d217169b981a61188920f9d28638709a5132201684d792b9264271b7f09157ed4321b1c097f7a4abecfc0977d40a7ee599c845883bd1074ca23c4af
# Expiração do token de acesso (30 minutos = 1800000ms)
JWT_EXPIRATION=1800000
# Expiração do refresh token (30 dias = 2592000000ms)
JWT_REFRESH_EXPIRATION=2592000000

# ========================
# OAUTH2 - GOOGLE
# ========================

# Configurações do Google OAuth2 (CONFIGURAR SE USAR OAuth2)
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
FRONTEND_CALLBACK_URI=http://localhost:3000/oauth/callback

# ========================
# EMAIL
# ========================

# Configurações de email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@techmel.com
```

### ⚠️ Variáveis Importantes para Desenvolvimento

#### JWT_SECRET
- **Desenvolvimento**: Use a chave fornecida acima
- **Produção**: SEMPRE gere uma nova chave segura

```bash
# Gerar nova chave JWT (para produção)
openssl rand -hex 64
```

#### Credenciais OAuth2
- Para testes básicos: deixe as variáveis como estão
- Para testes OAuth2: configure com suas credenciais Google
- Consulte [OAUTH2-AUTH.md](./OAUTH2-AUTH.md) para detalhes

## 4. Execução com Docker Compose

### 🚀 Inicialização Rápida

```bash
# 1. Ir para o diretório do projeto
cd tech-mel

# 2. Criar arquivo .env (ver seção anterior)

# 3. Construir e iniciar todos os serviços
docker-compose up --build

# OU executar em background
docker-compose up --build -d
```

### Comandos Úteis

```bash
# Ver logs de todos os serviços
docker-compose logs -f

# Ver logs apenas do backend
docker-compose logs -f tech-mel-backend

# Ver logs apenas do banco
docker-compose logs -f tech-mel-postgres

# Parar todos os serviços
docker-compose down

# Parar e remover volumes (CUIDADO: perde dados do banco)
docker-compose down -v

# Rebuild apenas do backend
docker-compose build tech-mel-backend

# Restart apenas de um serviço
docker-compose restart tech-mel-backend

# Entrar no container do backend
docker-compose exec tech-mel-backend sh

# Entrar no container do banco
docker-compose exec tech-mel-postgres psql -U techmel_user -d techmel_db
```

### Verificação da Inicialização

```bash
# Verificar se os containers estão rodando
docker-compose ps

# Deve mostrar algo como:
# NAME                COMMAND                  SERVICE             STATUS              PORTS
# techmel-backend     "java -jar app.jar"      tech-mel-backend    running (healthy)   0.0.0.0:8080->8080/tcp
# techmel-db          "docker-entrypoint.s…"   tech-mel-postgres   running (healthy)   0.0.0.0:5432->5432/tcp
# techmel-redis       "docker-entrypoint.s…"   tech-mel-redis      running (healthy)   6379/tcp
```

## 5. Execução Local (sem Docker)

### Pré-requisitos Locais

1. **Java 17** instalado
2. **Maven 3.8+** instalado
3. **PostgreSQL** instalado e rodando
4. **Redis** instalado e rodando

### exporte as variáveis de ambiente no terminal
### exemplo:
````bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=luistestes34@gmail.com
export MAIL_PASSWORD='rtgo xsyi nswy juik'
export MAIL_FROM=email...
export MAIL_AUTH=true
export MAIL_STARTTLS=true
...
````

### Configuração do Banco Local

```bash
# Conectar ao PostgreSQL como superusuário
sudo -u postgres psql

# Criar usuário e banco
CREATE USER techmel_user WITH ENCRYPTED PASSWORD 'techmel_password_2024';
CREATE DATABASE techmel_db OWNER techmel_user;
GRANT ALL PRIVILEGES ON DATABASE techmel_db TO techmel_user;
\q
```

### Configuração das Variáveis Locais

Altere o arquivo `.env` para execução local:

```bash
# Alterar URLs do banco e Redis para localhost
DB_URL=jdbc:postgresql://localhost:5432/techmel_db
REDIS_HOST=localhost

# Manter outras configurações iguais
```

### Execução da Aplicação

```bash
# 1. Compilar o projeto
mvn clean compile

# 2. Executar testes (opcional)
mvn test

# 3. Executar a aplicação em modo desenvolvimento
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# OU construir JAR e executar
mvn clean package -DskipTests
java -jar target/tech-mel-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 6. Perfis de Aplicação

### Perfis Disponíveis

| Perfil | Arquivo | Uso |
|--------|---------|-----|
| `dev` | `application-dev.properties` | Desenvolvimento local |
| `prod` | `application-prod.properties` | Produção |
| `default` | `application.properties` | Configurações base |

### Configurações por Perfil

#### Perfil DEV (Desenvolvimento)
- Logs detalhados habilitados
- DDL automático do Hibernate (`update`)
- Validações relaxadas
- CORS habilitado para localhost

#### Perfil PROD (Produção)
- Logs otimizados
- DDL desabilitado
- Validações rigorosas
- CORS restritivo

### Ativando Perfis

```bash
# Via Docker Compose (já configurado)
SPRING_PROFILE=dev

# Via execução local
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Via variável de ambiente
export SPRING_PROFILES_ACTIVE=dev
java -jar app.jar
```

## 7. Configuração de Usuários de Teste

### 🧪 Usuários Padrão para Testes

Após a inicialização, execute os seguintes comandos SQL para criar usuários de teste:

```bash
# Conectar ao banco via Docker
docker-compose exec tech-mel-postgres psql -U techmel_user -d techmel_db

# OU conectar localmente
psql -U techmel_user -d techmel_db -h localhost
```

### Script de Criação de Usuários

```sql
-- ========================
-- USUÁRIO TÉCNICO
-- ========================
INSERT INTO users (
    email, 
    password, 
    name, 
    email_verified, 
    role, 
    enabled, 
    auth_provider,
    available_hives,
    created_at,
    updated_at
) VALUES (
    'tecnico@techmel.com',
    '$2a$10$N.zmdr9k7uOCQb96VdqQ6OKLTq2N9DzRBEklc.L9gSHp3q7.LFHEu', -- senha: senha123
    'João Técnico Silva',
    true,
    'TECHNICIAN',
    true,
    'LOCAL',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ========================
-- USUÁRIO COMUM COM HIVES
-- ========================
INSERT INTO users (
    email, 
    password, 
    name, 
    email_verified, 
    role, 
    enabled, 
    auth_provider,
    available_hives,
    created_at,
    updated_at
) VALUES (
    'cliente@techmel.com',
    '$2a$10$N.zmdr9k7uOCQb96VdqQ6OKLTq2N9DzRBEklc.L9gSHp3q7.LFHEu', -- senha: senha123
    'Maria Cliente Santos',
    true,
    'COMMON',
    true,
    'LOCAL',
    5, -- 5 colmeias disponíveis para teste
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ========================
-- USUÁRIO ADMIN
-- ========================
INSERT INTO users (
    email, 
    password, 
    name, 
    email_verified, 
    role, 
    enabled, 
    auth_provider,
    available_hives,
    created_at,
    updated_at
) VALUES (
    'admin@techmel.com',
    '$2a$10$N.zmdr9k7uOCQb96VdqQ6OKLTq2N9DzRBEklc.L9gSHp3q7.LFHEu', -- senha: senha123
    'Admin TechMel',
    true,
    'ADMIN',
    true,
    'LOCAL',
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Verificar usuários criados
SELECT id, email, name, role, available_hives FROM users;
```

### 🔐 Credenciais de Teste

| Usuário | Email | Senha | Role | Hives Disponíveis |
|---------|-------|-------|------|-------------------|
| Técnico | `tecnico@techmel.com` | `senha123` | TECHNICIAN | 0 |
| Cliente | `cliente@techmel.com` | `senha123` | COMMON | 5 |
| Admin | `admin@techmel.com` | `senha123` | ADMIN | 0 |

## 8. Testes das APIs

### 🧪 Endpoints para Teste

#### Autenticação

```bash
# Login do técnico
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tecnico@techmel.com",
    "password": "senha123"
  }'

# Login do cliente
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cliente@techmel.com",
    "password": "senha123"
  }'
```

### 📋 Swagger UI

Acesse a documentação interativa da API:

```
http://localhost:8080/swagger-ui/index.html
```

- Interface gráfica para testar endpoints
- Documentação automática das APIs
- Possibilidade de executar requests diretamente

## 9. Monitoramento e Logs

### Logs da Aplicação

```bash
# Logs em tempo real via Docker
docker-compose logs -f tech-mel-backend

# Logs com filtro
docker-compose logs -f tech-mel-backend | grep ERROR

# Últimas 100 linhas
docker-compose logs --tail=100 tech-mel-backend
```

### Logs de Depuração

Para ativar logs detalhados, adicione ao `.env`:

```bash
# Logs de debug para desenvolvimento
LOGGING_LEVEL_COM_TECH_MEL=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
LOGGING_LEVEL_ORG_HIBERNATE_SQL=DEBUG
```

### Monitoramento do Banco

```bash
# Verificar conexões ativas
docker-compose exec tech-mel-postgres psql -U techmel_user -d techmel_db -c "SELECT count(*) FROM pg_stat_activity;"

# Ver tabelas criadas
docker-compose exec tech-mel-postgres psql -U techmel_user -d techmel_db -c "\dt"

# Ver dados dos usuários
docker-compose exec tech-mel-postgres psql -U techmel_user -d techmel_db -c "SELECT email, role, available_hives FROM users;"
```

## 10. Troubleshooting

### Problemas Comuns

#### 1. "Porta 8080 já está em uso"

```bash
# Verificar o que está usando a porta
sudo lsof -i :8080

# Parar processo se necessário
sudo kill -9 PID_DO_PROCESSO

# OU alterar a porta no docker-compose.yml
ports:
  - "8081:8080"  # Usar porta 8081 externamente
```

#### 2. "Erro de conexão com banco de dados"

```bash
# Verificar se o PostgreSQL está rodando
docker-compose ps tech-mel-postgres

# Verificar logs do banco
docker-compose logs tech-mel-postgres

# Resetar banco (perde dados)
docker-compose down -v
docker-compose up tech-mel-postgres -d
```

#### 3. "JWT Token inválido"

- Verificar se JWT_SECRET está configurado corretamente
- Token pode ter expirado (30 minutos por padrão)
- Fazer novo login para obter token válido

#### 4. "Redis connection failed"

```bash
# Verificar Redis
docker-compose ps tech-mel-redis

# Testar conexão Redis
docker-compose exec tech-mel-redis redis-cli ping
# Deve responder: PONG
```

#### 5. "Build failed"

```bash
# Limpar cache do Docker
docker system prune -a

# Rebuild completo
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Comandos de Limpeza

```bash
# Limpeza completa (CUIDADO: remove todos os dados)
docker-compose down -v
docker system prune -a
docker volume prune

# Resetar apenas dados da aplicação
docker-compose down -v
docker-compose up --build
```

---

## 📞 Suporte

Para problemas não cobertos neste guia:

1. Verifique os logs da aplicação
2. Consulte a documentação específica dos módulos
3. Verifique as issues do repositório
4. Entre em contato com a equipe de desenvolvimento

## 🔗 Documentação Relacionada

- [Módulo de Hives](./HIVES-MODULE.md)
- [Autenticação OAuth2](./OAUTH2-AUTH.md)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
