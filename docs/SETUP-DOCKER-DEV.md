# Setup Desenvolvimento com Docker - TechMel

## 🚀 Setup Rápido (5 minutos)

### 1. Pré-requisitos

- **Docker** e **Docker Compose** instalados
- **Git** para clonar o repositório

### 2. Configuração

```bash
# 1. Clonar o repositório
git clone <repository-url>
cd tech-mel

# 2. Criar arquivo .env na raiz do projeto
touch .env
```

### 3. Configurar .env

Adicione apenas estas 5 variáveis no arquivo `.env`:

```bash
MAIL_USERNAME=seu-email@gmail.com
MAIL_PASSWORD=sua-senha-de-app-gmail
MAIL_FROM=seu-email@gmail.com
APP_ADMIN_EMAIL=admin@sua-empresa.com
APP_ADMIN_PASSWORD=SuaSenhaSegura123
```

> **📧 Importante**: Use uma [senha de aplicativo do Gmail](https://support.google.com/accounts/answer/185833), não sua senha normal!

### 4. Executar

```bash
# Iniciar todos os serviços
docker-compose up --build

# OU em background
docker-compose up --build -d
```

### 5. Testar

- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui/index.html
- **Admin**: Use as credenciais configuradas em `APP_ADMIN_EMAIL` e `APP_ADMIN_PASSWORD`

## 📋 Comandos Úteis

```bash
# Ver logs
docker-compose logs -f

# Parar tudo
docker-compose down

# Resetar dados (cuidado!)
docker-compose down -v

# Rebuild apenas backend
docker-compose build techmel-backend
```

## 🔧 Troubleshooting

### Porta 8080 ocupada?

```bash
# Verificar o que está usando
sudo lsof -i :8080

# Matar processo
sudo kill -9 PID
```

### Erro de email?

- Verifique se está usando senha de aplicativo do Gmail
- Ative 2FA na sua conta Google primeiro

### Limpar tudo e recomeçar

```bash
docker-compose down -v
docker system prune -a
docker-compose up --build
```

---

**Pronto!** 🎉 Com esses comandos sua aplicação estará rodando localmente.
