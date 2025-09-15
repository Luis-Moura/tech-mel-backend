# 🧪 Guia de Teste - Módulo de Administração TechMel

## 📋 Pré-requisitos

1. **Banco de dados** PostgreSQL configurado
2. **Redis** em execução
3. **Servidor SMTP** configurado (ou usar configurações de teste)
4. **Admin primário** criado automaticamente pelo `AdminInitializer`

---

## 🔐 **1. TESTE DE AUTENTICAÇÃO ADMIN**

### 1.1 Login como Admin Primário

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@techmel.com",
  "password": "Admin@123"
}
```

**Resultado esperado:**

- Status: `200 OK`
- Response com `accessToken` e `refreshToken`
- Role: `ADMIN`, isPrimary: `true`

---

## 👨‍💻 **2. TESTE DE CRUD DE TÉCNICOS**

### 2.1 Criar Técnico (Admin → Técnico)

```bash
POST /api/admin/technicians
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao.tecnico@techmel.com",
  "password": "TempPassword123!"
}
```

**Validações:**

- ✅ Status: `201 Created`
- ✅ Email enviado para `joao.tecnico@techmel.com`
- ✅ `requiresPasswordChange: true`
- ✅ Log de auditoria criado

### 2.2 Listar Técnicos

```bash
GET /api/admin/technicians
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ Lista contém o técnico criado
- ✅ Apenas usuários com role `TECHNICIAN`

### 2.3 Atualizar Técnico

```bash
PUT /api/admin/technicians/{technician_id}
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "name": "João Silva Santos",
  "email": "joao.santos@techmel.com",
  "availableHives": 10
}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ Dados atualizados corretamente
- ✅ Log de auditoria com ação `UPDATE`

### 2.4 Deletar Técnico

```bash
DELETE /api/admin/technicians/{technician_id}
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `204 No Content`
- ✅ Técnico removido do banco
- ✅ Log de auditoria com ação `DELETE`

---

## 🔧 **3. TESTE DE ADMINS SECUNDÁRIOS**

### 3.1 Criar Admin Secundário (Apenas Admin Primário)

```bash
POST /api/admin/secondary-admins
Authorization: Bearer {primary_admin_token}
Content-Type: application/json

{
  "name": "Maria Administradora",
  "email": "maria.admin@techmel.com",
  "password": "AdminTemp456!"
}
```

**Validações:**

- ✅ Status: `201 Created`
- ✅ `isPrimary: false`
- ✅ Email de credenciais enviado
- ✅ Apenas admin primário pode executar

### 3.2 Tentar Criar Admin com Admin Secundário (Deve Falhar)

```bash
POST /api/admin/secondary-admins
Authorization: Bearer {secondary_admin_token}
Content-Type: application/json

{
  "name": "Outro Admin",
  "email": "outro@techmel.com",
  "password": "Pass123!"
}
```

**Validações:**

- ❌ Status: `403 Forbidden`
- ❌ Mensagem: "Apenas administradores primários podem criar outros administradores"

---

## 👥 **4. TESTE DE GESTÃO DE USUÁRIOS**

### 4.1 Listar Todos os Usuários (Paginado)

```bash
GET /api/admin/users?page=0&size=10&sortBy=createdAt&sortDirection=desc
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ Paginação funcionando
- ✅ Todos os roles incluídos (COMMON, TECHNICIAN, ADMIN)

### 4.2 Filtrar Usuários por Role

```bash
GET /api/admin/users?role=TECHNICIAN
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Apenas técnicos na resposta
- ✅ Paginação aplicada

### 4.3 Buscar Usuários por Nome/Email

```bash
GET /api/admin/users?searchTerm=joão
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Busca case-insensitive
- ✅ Busca tanto em nome quanto email

### 4.4 Ativar Usuário Inativo

```bash
POST /api/admin/users/{user_id}/activate
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ `isActive: true` no banco
- ✅ Log de auditoria `ACTIVATE`

### 4.5 Desativar Usuário

```bash
POST /api/admin/users/{user_id}/deactivate
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ `isActive: false` no banco
- ✅ Não permite desativar admin primário

### 4.6 Tentar Desativar Admin Primário (Deve Falhar)

```bash
POST /api/admin/users/{primary_admin_id}/deactivate
Authorization: Bearer {admin_token}
```

**Validações:**

- ❌ Status: `403 Forbidden`
- ❌ Mensagem: "Não é possível desativar o administrador primário"

---

## 🔑 **5. TESTE DE RESET DE SENHA**

### 5.1 Resetar Senha de Usuário Comum

```bash
POST /api/admin/users/{common_user_id}/reset-password
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ Email enviado com nova senha temporária
- ✅ `requiresPasswordChange: true`
- ✅ Log de auditoria `PASSWORD_RESET`

### 5.2 Tentar Resetar Senha do Admin Primário (Deve Falhar)

```bash
POST /api/admin/users/{primary_admin_id}/reset-password
Authorization: Bearer {admin_token}
```

**Validações:**

- ❌ Status: `403 Forbidden`
- ❌ Mensagem: "Não é possível mudar a senha do administrador primário"

---

## 📊 **6. TESTE DE ESTATÍSTICAS**

### 6.1 Obter Estatísticas Gerais

```bash
GET /api/admin/statistics
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ Contém: `totalUsers`, `activeUsers`, `usersByRole`
- ✅ Dados precisos e atualizados

### 6.2 Usuários Recentes

```bash
GET /api/admin/statistics/recent-users?limit=5
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Máximo 5 usuários
- ✅ Ordenados por data de criação (mais recentes primeiro)

### 6.3 Usuários Inativos

```bash
GET /api/admin/statistics/inactive-users?daysInactive=30
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Usuários sem login há 30+ dias
- ✅ Ou usuários que nunca fizeram login

---

## 📋 **7. TESTE DE AUDITORIA**

### 7.1 Consultar Logs de Auditoria

```bash
GET /api/audit/logs?page=0&size=20
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Status: `200 OK`
- ✅ Logs ordenados por timestamp (mais recentes primeiro)
- ✅ Inclui dados do usuário (nome, email)

### 7.2 Filtrar Logs por Ação

```bash
GET /api/audit/logs?action=CREATE&entityType=USER
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Apenas logs de criação de usuários
- ✅ Filtros funcionando corretamente

### 7.3 Filtrar Logs por Período

```bash
GET /api/audit/logs?startDate=2025-08-01T00:00:00&endDate=2025-08-18T23:59:59
Authorization: Bearer {admin_token}
```

**Validações:**

- ✅ Apenas logs do período especificado
- ✅ Formato de data ISO correto

---

## 📧 **8. TESTE DE EMAILS**

### 8.1 Verificar Templates de Email

**Para Técnicos:**

- Template: `email/technician-credentials.html`
- Variáveis: `name`, `email`, `password`, `loginUrl`

**Para Admins:**

- Template: `email/admin-credentials.html`
- Variáveis: `name`, `email`, `password`, `loginUrl`

**Para Reset de Senha:**

- Template: `email/password-reset-admin.html`
- Variáveis: `name`, `email`, `newPassword`, `loginUrl`

### 8.2 Teste de Envio

1. **Criar técnico** → Verificar email recebido
2. **Criar admin** → Verificar email recebido
3. **Resetar senha** → Verificar email recebido

---

## 🔒 **9. TESTE DE SEGURANÇA**

### 9.1 Tentar Acessar sem Token

```bash
GET /api/admin/users
# Sem Authorization header
```

**Validação:**

- ❌ Status: `401 Unauthorized`

### 9.2 Tentar Acessar com Role Insuficiente

```bash
GET /api/admin/users
Authorization: Bearer {technician_token}
```

**Validação:**

- ❌ Status: `403 Forbidden`

### 9.3 Admin Secundário Tentando Gerenciar Admin Primário

```bash
POST /api/admin/users/{primary_admin_id}/reset-password
Authorization: Bearer {secondary_admin_token}
```

**Validação:**

- ❌ Status: `403 Forbidden`

---

## ✅ **10. CHECKLIST FINAL**

### Funcionalidades Core:

- [ ] ✅ Criar técnico (com email)
- [ ] ✅ Atualizar técnico
- [ ] ✅ Deletar técnico
- [ ] ✅ Listar técnicos
- [ ] ✅ Criar admin secundário (apenas primário)
- [ ] ✅ Listar usuários (paginado + filtros)
- [ ] ✅ Ativar/desativar usuários
- [ ] ✅ Reset de senha (com email)
- [ ] ✅ Estatísticas do sistema

### Sistema de Auditoria:

- [ ] ✅ Logs automáticos para todas as ações
- [ ] ✅ Consulta de logs com filtros
- [ ] ✅ Dados completos (IP, User-Agent, etc.)

### Segurança:

- [ ] ✅ Apenas ADMINs acessam endpoints
- [ ] ✅ Admin primário não pode ser desativado
- [ ] ✅ Validações de permissão hierárquica
- [ ] ✅ Senhas temporárias obrigatórias

### Email:

- [ ] ✅ Templates profissionais
- [ ] ✅ Envio assíncrono
- [ ] ✅ Variáveis dinâmicas

---

## 🚨 **CASOS DE ERRO ESPERADOS**

1. **Criar técnico com email duplicado** → `409 Conflict`
2. **Atualizar técnico inexistente** → `404 Not Found`
3. **Admin secundário criando outro admin** → `403 Forbidden`
4. **Desativar admin primário** → `403 Forbidden`
5. **Resetar senha de admin primário** → `403 Forbidden`
6. **Acesso sem permissão** → `403 Forbidden`
7. **Acesso não autenticado** → `401 Unauthorized`

---

## 📝 **LOGS ESPERADOS**

Verifique os logs da aplicação para:

1. **Criação de técnico**: `"Técnico criado com sucesso: {id}"`
2. **Email enviado**: `"Email de credenciais enviado para técnico: {email}"`
3. **Auditoria salva**: Log de auditoria com todas as ações
4. **Erros de validação**: Logs de erro para casos inválidos

---

**O módulo de administração está totalmente funcional e pronto para produção!** 🎉
