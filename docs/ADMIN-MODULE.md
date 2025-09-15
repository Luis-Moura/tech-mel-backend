# Documentação Técnica: Módulo de Administração (Admin) no TechMel

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura da Solução](#2-arquitetura-da-solução)
3. [Entidades e Relacionamentos](#3-entidades-e-relacionamentos)
4. [Casos de Uso (Use Cases)](#4-casos-de-uso-use-cases)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [Integração entre Módulos](#6-integração-entre-módulos)
7. [Implementação das APIs](#7-implementação-das-apis)
8. [Controle de Acesso e Segurança](#8-controle-de-acesso-e-segurança)
9. [Gestão de Credenciais](#9-gestão-de-credenciais)
10. [Exemplos de Interação](#10-exemplos-de-interação)

## 1. Visão Geral

O módulo de Administração é o **centro de controle administrativo** do TechMel, responsável pela gestão completa de usuários, permissões e operações privilegiadas do sistema. Este módulo permite que administradores gerenciem técnicos, usuários comuns, configurações do sistema e acessem estatísticas operacionais.

### Características Principais

- **Gestão de Usuários**: Criação, atualização, ativação/desativação de contas
- **Controle de Permissões**: Gerenciamento de roles e privilégios
- **Gestão de Técnicos**: Criação e manutenção de contas técnicas
- **Administradores Secundários**: Criação de admins não-primários
- **Estatísticas do Sistema**: Dashboards e métricas operacionais
- **Auditoria Integrada**: Registro automático de todas as ações administrativas
- **Segurança Avançada**: Proteções contra operações críticas

### Hierarquia de Privilégios

```
┌─────────────────────┐
│ Admin Primário      │ ← Máximo privilégio, não pode ser removido
│ (isPrimary = true)  │   Única instância no sistema
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ Admin Secundário    │ ← Privilégios administrativos completos
│ (isPrimary = false) │   Criado pelo admin primário
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ Técnico             │ ← Gestão de colmeias e dispositivos IoT
│ (ROLE_TECHNICIAN)   │   Criado por qualquer admin
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ Usuário Comum       │ ← Visualização e gestão de suas colmeias
│ (ROLE_COMMON)       │   Auto-registro ou criação via admin
└─────────────────────┘
```

## 2. Arquitetura da Solução

### Diagrama de Componentes

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Admin         │    │   Admin         │    │   PostgreSQL    │
│   Dashboard     │    │   Service       │    │   Database      │
│                 │    │                 │    │                 │
│ • Gestão Users  │◄──►│ • CRUD Users    │◄──►│ • users         │
│ • Estatísticas  │    │ • Validações    │    │ • audit_logs    │
│ • Auditoria     │    │ • Segurança     │    │ • refresh_tokens│
│ • Relatórios    │    │ • Email Notif   │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Email         │
                       │   Service       │
                       │                 │
                       │ • Credenciais   │
                       │ • Reset Senha   │
                       │ • Notificações  │
                       └─────────────────┘
```

### Componentes Principais

1. **AdminController**: Endpoints REST para operações administrativas
2. **AdminService**: Lógica de negócio e orquestração
3. **UserRepositoryPort**: Interface para persistência de usuários
4. **EmailSenderPort**: Interface para envio de notificações
5. **AuditService**: Integração para logging de ações administrativas
6. **AuthenticationUtil**: Controle de identidade e permissões

## 3. Entidades e Relacionamentos

### Entidade User (Campos Administrativos)

```java
public class User {
    private UUID id;                    // Identificador único
    private String email;               // Email único no sistema
    private String name;                // Nome completo
    private String password;            // Senha hasheada (BCrypt)
    private Role role;                  // ADMIN, TECHNICIAN, COMMON
    private boolean emailVerified;      // Email confirmado
    private boolean enabled;            // Conta habilitada
    private boolean locked;             // Conta bloqueada
    private boolean isActive;           // Status ativo/inativo
    private boolean isPrimary;          // Admin primário (único)
    private boolean requiresPasswordChange; // Forçar mudança de senha
    private AuthProvider authProvider;  // LOCAL, GOOGLE, etc.
    private Integer availableHives;     // Colmeias disponíveis
    private LocalDateTime lastLogin;    // Último acesso
    private LocalDateTime createdAt;    // Data de criação
    private LocalDateTime updatedAt;    // Data de atualização
}
```

### Estados e Transições

```
┌─────────────┐    CREATE      ┌─────────────┐    ACTIVATE    ┌─────────────┐
│    NOVO     │ ──────────────► │   CRIADO    │ ──────────────► │   ATIVO     │
│             │                │ (inactive)  │                │  (active)   │
└─────────────┘                └─────────────┘                └─────────────┘
                                       │                              │
                                   LOCK │                              │ DEACTIVATE
                                       ▼                              ▼
                               ┌─────────────┐                ┌─────────────┐
                               │ BLOQUEADO   │                │  INATIVO    │
                               │  (locked)   │                │ (inactive)  │
                               └─────────────┘                └─────────────┘
                                       │                              │
                                 UNLOCK │                     ACTIVATE │
                                       └──────────────────────────────┘
```

### Regras de Integridade

- **Email único**: Constraint unique na coluna email
- **Admin primário único**: Apenas um usuário com `isPrimary = true`
- **Roles válidos**: ADMIN, TECHNICIAN, COMMON apenas
- **Proteção admin primário**: Não pode ser deletado, desativado ou bloqueado

## 4. Casos de Uso (Use Cases)

### Interface AdminUseCase

```java
public interface AdminUseCase {
    // Gestão de Técnicos
    User createTechnician(String email, String name, String password);
    User updateTechnician(UUID technicianId, String name, String email, Integer availableHives);
    void deleteTechnician(UUID technicianId);
    List<User> getAllTechnicians();

    // Gestão de Administradores
    User createSecondaryAdmin(String email, String name, String password);
    List<User> getAllAdmins();

    // Gestão Geral de Usuários
    Page<User> getAllUsers(Pageable pageable);
    Page<User> getUsersByRole(Role role, Pageable pageable);
    Page<User> searchUsers(String searchTerm, Pageable pageable);
    void activateUser(UUID userId);
    void deactivateUser(UUID userId);
    void resetUserPassword(UUID userId);
    void unlockUser(UUID userId);
    void lockUser(UUID userId);

    // Estatísticas e Relatórios
    Map<String, Object> getSystemStatistics();
    List<User> getRecentlyRegisteredUsers(int limit);
    List<User> getInactiveUsers(int daysInactive);

    // Controle de Acesso
    boolean isPrimaryAdmin(UUID userId);
    boolean canManageAdmins(UUID currentUserId);
}
```

### Casos de Uso Detalhados

#### UC01: Criação de Técnico

- **Ator**: Administrador
- **Pré-condição**: Usuário com role ADMIN
- **Fluxo Principal**:
  1. Admin fornece email, nome e opcionalmente senha
  2. Sistema valida unicidade do email
  3. Sistema gera senha temporária se não fornecida
  4. Sistema cria usuário com role TECHNICIAN
  5. Sistema envia email com credenciais
  6. Sistema registra ação na auditoria
  7. Retorna dados do técnico criado

#### UC02: Gestão de Usuários

- **Ator**: Administrador
- **Pré-condição**: Usuário com role ADMIN
- **Operações Disponíveis**:
  - Ativar/Desativar contas
  - Bloquear/Desbloquear usuários
  - Resetar senhas
  - Forçar mudança de senha
  - Buscar e filtrar usuários

#### UC03: Criação de Admin Secundário

- **Ator**: Administrador Primário
- **Pré-condição**: Usuário com `isPrimary = true`
- **Fluxo Principal**:
  1. Admin primário fornece dados do novo admin
  2. Sistema valida privilégios do solicitante
  3. Sistema cria usuário com role ADMIN e `isPrimary = false`
  4. Sistema envia credenciais por email
  5. Sistema registra na auditoria

#### UC04: Estatísticas do Sistema

- **Ator**: Administrador
- **Pré-condição**: Usuário com role ADMIN
- **Métricas Fornecidas**:
  - Total de usuários por role
  - Usuários ativos/inativos
  - Novos registros no último mês
  - Usuários inativos por período
  - Estatísticas de crescimento

## 5. Fluxo de Dados

### Fluxo de Criação de Técnico

```
1. Admin Dashboard
   │ POST /api/admin/technicians
   │ Body: {email, name, password?}
   ▼
2. AdminController
   │ @PreAuthorize("hasRole('ADMIN')")
   │ @Valid CreateTechnicianRequest
   ▼
3. AdminService.createTechnician()
   │ ├─ userRepositoryPort.findByEmail(email) // validar unicidade
   │ ├─ generateTemporaryPassword() // se não fornecida
   │ ├─ passwordEncoder.encode(password)
   │ ├─ build User with TECHNICIAN role
   │ ├─ userRepositoryPort.save(user)
   │ ├─ emailSenderPort.sendTechnicianCredentialsEmail()
   │ └─ auditUseCase.logAction(CREATE, USER, details)
   ▼
4. Email Notification
   │ Template: technician-credentials.html
   │ Dados: email, nome, senha temporária
   ▼
5. Audit Log
   │ Action: CREATE
   │ Entity: USER
   │ Details: "Técnico criado: email@domain.com"
   ▼
6. Response HTTP 201
   │ TechnicianResponse with full user data
```

### Fluxo de Reset de Senha

```
1. Admin Action
   │ POST /api/admin/users/{userId}/reset-password
   ▼
2. Security Validation
   │ ├─ validate hasRole('ADMIN')
   │ ├─ validate target user exists
   │ └─ validate target is not primary admin
   ▼
3. Password Reset Process
   │ ├─ generateTemporaryPassword()
   │ ├─ passwordEncoder.encode(temporaryPassword)
   │ ├─ user.setRequiresPasswordChange(true)
   │ ├─ userRepositoryPort.save(user)
   │ └─ emailSenderPort.sendPasswordResetEmail()
   ▼
4. Audit & Response
   │ ├─ auditUseCase.logAction(PASSWORD_RESET, USER, details)
   │ └─ HTTP 200 OK
```

### Fluxo de Estatísticas

```
1. Dashboard Request
   │ GET /api/admin/statistics
   ▼
2. Data Aggregation
   │ ├─ userRepositoryPort.countByRole(ADMIN)
   │ ├─ userRepositoryPort.countByRole(TECHNICIAN)
   │ ├─ userRepositoryPort.countByRole(COMMON)
   │ ├─ userRepositoryPort.countByIsActive(true/false)
   │ └─ userRepositoryPort.countByCreatedAtAfter(lastMonth)
   ▼
3. Response Building
   │ SystemStatisticsResponse {
   │   totalUsers, activeUsers, inactiveUsers,
   │   usersByRole, newUsersLastMonth, generatedAt
   │ }
   ▼
4. JSON Response
   │ Dashboard metrics for visualization
```

## 6. Integração entre Módulos

### Admin → Audit (Logging Automático)

```java
// Todas as ações administrativas são logadas
auditUseCase.logAction(
    getCurrentAdminId(),
    AuditAction.CREATE,
    EntityType.USER,
    savedUser.getId().toString(),
    "Técnico criado: " + savedUser.getEmail()
);
```

**Características da Integração**:

- **Automática**: Todas as operações administrativas geram logs
- **Assíncrona**: Não bloqueia operações principais
- **Detalhada**: Inclui valores antigos e novos para atualizações
- **Rastreável**: Identifica o admin que executou a ação

### Admin → Email (Notificações)

```java
// Envio de credenciais para novos usuários
emailSenderPort.sendTechnicianCredentialsEmail(
    user.getEmail(),
    user.getName(),
    temporaryPassword
);
```

**Tipos de Notificações**:

- Credenciais para técnicos
- Credenciais para admins secundários
- Reset de senha por admin
- Ativação/desativação de conta

### Admin → Hive (Via User Management)

```java
// Atualização de colmeias disponíveis para técnicos
technician.setAvailableHives(newAvailableHives);
```

**Impacto Indireto**:

- Alteração de `availableHives` afeta capacidade de criação de colmeias
- Desativação de técnico pode impactar operações de campo
- Bloqueio de usuário impede acesso às suas colmeias

## 7. Implementação das APIs

### Endpoints Principais

#### POST /api/admin/technicians

**Criar novo técnico**

```bash
curl -X POST http://localhost:8080/api/admin/technicians \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tecnico@techmel.com",
    "name": "João Técnico Silva",
    "password": "MinhaSenh@123"
  }'
```

**Resposta**:

```json
{
	"id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
	"email": "tecnico@techmel.com",
	"name": "João Técnico Silva",
	"role": "TECHNICIAN",
	"isActive": true,
	"isLocked": false,
	"emailVerified": true,
	"requiresPasswordChange": false,
	"availableHives": 10,
	"createdAt": "2025-01-15T10:30:00",
	"updatedAt": "2025-01-15T10:30:00"
}
```

#### GET /api/admin/users

**Listar usuários com filtros**

```bash
curl -X GET \
  'http://localhost:8080/api/admin/users?page=0&size=20&sortBy=createdAt&sortDirection=desc&role=TECHNICIAN' \
  -H 'Authorization: Bearer {admin-token}'
```

#### POST /api/admin/users/{userId}/reset-password

**Resetar senha de usuário**

```bash
curl -X POST \
  'http://localhost:8080/api/admin/users/f47ac10b-58cc-4372-a567-0e02b2c3d479/reset-password' \
  -H 'Authorization: Bearer {admin-token}'
```

#### GET /api/admin/statistics

**Estatísticas do sistema**

```bash
curl -X GET \
  'http://localhost:8080/api/admin/statistics' \
  -H 'Authorization: Bearer {admin-token}'
```

**Resposta**:

```json
{
	"totalUsers": 1250,
	"activeUsers": 1180,
	"inactiveUsers": 70,
	"adminUsers": 3,
	"technicianUsers": 25,
	"commonUsers": 1222,
	"newUsersLastMonth": 145,
	"usersByRole": {
		"ADMIN": 3,
		"TECHNICIAN": 25,
		"COMMON": 1222
	},
	"usersByStatus": {
		"active": 1180,
		"inactive": 70
	},
	"generatedAt": "2025-01-15T14:30:00"
}
```

### Códigos de Resposta

- **200 OK**: Operação realizada com sucesso
- **201 Created**: Usuário criado com sucesso
- **204 No Content**: Operação de atualização/deleção concluída
- **400 Bad Request**: Dados inválidos ou violação de regras
- **401 Unauthorized**: Token inválido ou expirado
- **403 Forbidden**: Privilégios insuficientes
- **404 Not Found**: Usuário não encontrado
- **409 Conflict**: Email já em uso

## 8. Controle de Acesso e Segurança

### Matriz de Permissões

| Operação         | Admin Primário | Admin Secundário | Técnico | Comum |
| ---------------- | -------------- | ---------------- | ------- | ----- |
| Criar Técnico    | ✅             | ✅               | ❌      | ❌    |
| Criar Admin      | ✅             | ❌               | ❌      | ❌    |
| Reset Senha      | ✅¹            | ✅¹              | ❌      | ❌    |
| Bloquear Usuário | ✅¹            | ✅¹              | ❌      | ❌    |
| Ver Estatísticas | ✅             | ✅               | ❌      | ❌    |
| Deletar Técnico  | ✅             | ✅               | ❌      | ❌    |

¹ Exceto admin primário

### Proteções Especiais

#### Admin Primário Protegido

```java
// Validações para proteger admin primário
if (user.getRole() == User.Role.ADMIN && user.isPrimary()) {
    throw new ForbiddenException("Não é possível [operação] o administrador primário");
}
```

**Proteções Aplicadas**:

- ❌ Não pode ser deletado
- ❌ Não pode ser desativado
- ❌ Não pode ser bloqueado
- ❌ Não pode ter senha resetada por outros
- ❌ Não pode ter role alterado

#### Validações de Segurança

```java
// Validação de email único
if (userRepositoryPort.existsByEmailAndIdNot(email, userId)) {
    throw new ConflictException("Email já está em uso");
}

// Validação de privilégios para criar admin
if (!canManageAdmins(currentUserId)) {
    throw new ForbiddenException("Apenas administradores primários podem criar outros administradores");
}
```

### Auditoria de Segurança

```java
// Log de todas as ações administrativas
auditUseCase.logAction(
    getCurrentAdminId(),
    AuditAction.ACCOUNT_LOCKED,
    EntityType.USER,
    userId.toString(),
    "Usuário bloqueado: " + user.getEmail()
);
```

## 9. Gestão de Credenciais

### Geração de Senhas Temporárias

```java
private static final String TEMP_PASSWORD_CHARACTERS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
private static final int TEMP_PASSWORD_LENGTH = 12;

private String generateTemporaryPassword() {
    SecureRandom random = new SecureRandom();
    StringBuilder password = new StringBuilder();

    for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
        password.append(TEMP_PASSWORD_CHARACTERS.charAt(
            random.nextInt(TEMP_PASSWORD_CHARACTERS.length())
        ));
    }

    return password.toString();
}
```

**Características**:

- **12 caracteres**: Comprimento adequado para segurança
- **Caracteres especiais**: Inclui símbolos para maior entropia
- **SecureRandom**: Gerador criptograficamente seguro
- **Força mudança**: `requiresPasswordChange = true`

### Templates de Email

#### Credenciais para Técnico

```html
<!-- technician-credentials.html -->
<h2>Bem-vindo ao TechMel - Credenciais de Técnico</h2>
<p>Olá {{name}},</p>
<p>Sua conta de técnico foi criada com sucesso.</p>
<div class="credentials">
	<p><strong>Email:</strong> {{email}}</p>
	<p><strong>Senha Temporária:</strong> {{password}}</p>
</div>
<p>⚠️ Você deverá alterar sua senha no primeiro acesso.</p>
```

#### Reset de Senha

```html
<!-- password-reset-admin.html -->
<h2>Senha Resetada - TechMel</h2>
<p>Olá {{name}},</p>
<p>Sua senha foi resetada por um administrador.</p>
<div class="credentials">
	<p><strong>Nova Senha Temporária:</strong> {{password}}</p>
</div>
<p>Por favor, faça login e altere sua senha imediatamente.</p>
```

### Políticas de Senha

```java
// Configuração BCrypt para hashing
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // Força 12 (alta segurança)
}
```

**Requisitos**:

- **Hashing BCrypt**: Força 12 para alta segurança
- **Senhas temporárias**: Força mudança no primeiro acesso
- **Unicidade**: Não permite reutilização de senhas antigas
- **Complexidade**: Senhas geradas incluem caracteres especiais

## 10. Exemplos de Interação

### Cenário 1: Setup Inicial do Sistema

#### Passo 1: Admin Primário Criando Primeiro Técnico

```bash
# Admin primário (criado na inicialização) cria primeiro técnico
POST /api/admin/technicians
{
  "email": "tecnico.campo@techmel.com",
  "name": "Carlos Técnico de Campo"
  // password não fornecida = geração automática
}

# Sistema responde:
HTTP 201 Created
{
  "id": "tech-uuid-123",
  "email": "tecnico.campo@techmel.com",
  "name": "Carlos Técnico de Campo",
  "role": "TECHNICIAN",
  "requiresPasswordChange": true,
  "availableHives": 10
}
```

#### Passo 2: Email Automático Enviado

```
Para: tecnico.campo@techmel.com
Assunto: Bem-vindo ao TechMel - Credenciais de Técnico

Olá Carlos Técnico de Campo,

Sua conta de técnico foi criada com sucesso no sistema TechMel.

Credenciais de Acesso:
Email: tecnico.campo@techmel.com
Senha Temporária: Kx9#mP2$vN7@

⚠️ IMPORTANTE: Você deverá alterar sua senha no primeiro acesso.

Acesse: https://app.techmel.com/login
```

#### Passo 3: Primeiro Login do Técnico

```bash
# Técnico faz login com senha temporária
POST /api/auth/login
{
  "email": "tecnico.campo@techmel.com",
  "password": "Kx9#mP2$vN7@"
}

# Sistema responde:
{
  "accessToken": "eyJ...",
  "refreshToken": "...",
  "requiresPasswordChange": true,  # ← Força mudança
  "user": {
    "role": "TECHNICIAN",
    "requiresPasswordChange": true
  }
}
```

### Cenário 2: Gestão de Usuário Problemático

#### Situação: Usuário com Comportamento Suspeito

```bash
# Admin identifica usuário suspeito via auditoria
GET /api/admin/audit/logs/user/user-suspeito-uuid

# Admin decide bloquear temporariamente
POST /api/admin/users/user-suspeito-uuid/lock

# Resposta do sistema:
HTTP 200 OK

# Log de auditoria gerado automaticamente:
{
  "action": "ACCOUNT_LOCKED",
  "entityType": "USER",
  "entityId": "user-suspeito-uuid",
  "details": "Usuário bloqueado: suspeito@email.com",
  "userId": "admin-uuid",
  "userName": "Admin Principal"
}
```

#### Investigação e Desbloqueio

```bash
# Após investigação, admin desbloqueia e reseta senha
POST /api/admin/users/user-suspeito-uuid/unlock
POST /api/admin/users/user-suspeito-uuid/reset-password

# Usuário recebe email com nova senha temporária
# Sistema força mudança de senha no próximo login
```

### Cenário 3: Dashboard Administrativo

#### Implementação Frontend

```typescript
interface AdminDashboardProps {}

const AdminDashboard: React.FC<AdminDashboardProps> = () => {
	const [statistics, setStatistics] = useState<SystemStatistics | null>(null);
	const [recentUsers, setRecentUsers] = useState<User[]>([]);

	useEffect(() => {
		const fetchDashboardData = async () => {
			try {
				// Buscar estatísticas gerais
				const statsResponse = await api.get("/admin/statistics");
				setStatistics(statsResponse.data);

				// Buscar usuários recentes
				const usersResponse = await api.get(
					"/admin/statistics/recent-users?limit=5"
				);
				setRecentUsers(usersResponse.data);
			} catch (error) {
				toast.error("Erro ao carregar dashboard administrativo");
			}
		};

		fetchDashboardData();
	}, []);

	return (
		<div className="admin-dashboard">
			<h1>Dashboard Administrativo</h1>

			{/* Métricas Principais */}
			<div className="metrics-grid">
				<MetricCard
					title="Total de Usuários"
					value={statistics?.totalUsers}
					icon="👥"
					trend={`+${statistics?.newUsersLastMonth} este mês`}
				/>
				<MetricCard
					title="Usuários Ativos"
					value={statistics?.activeUsers}
					icon="✅"
					percentage={(statistics?.activeUsers / statistics?.totalUsers) * 100}
				/>
				<MetricCard
					title="Técnicos"
					value={statistics?.technicianUsers}
					icon="🔧"
				/>
				<MetricCard
					title="Administradores"
					value={statistics?.adminUsers}
					icon="⚙️"
				/>
			</div>

			{/* Gráfico de Distribuição por Role */}
			<div className="chart-section">
				<h3>Distribuição de Usuários por Tipo</h3>
				<PieChart data={statistics?.usersByRole} />
			</div>

			{/* Usuários Recentes */}
			<div className="recent-users">
				<h3>Usuários Registrados Recentemente</h3>
				<UserTable users={recentUsers} showActions={true} />
			</div>

			{/* Ações Rápidas */}
			<div className="quick-actions">
				<QuickActionCard
					title="Criar Técnico"
					description="Adicionar novo técnico ao sistema"
					action={() => openCreateTechnicianModal()}
					icon="👷"
				/>
				<QuickActionCard
					title="Ver Auditoria"
					description="Acessar logs de auditoria"
					action={() => navigateToAudit()}
					icon="📋"
				/>
				<QuickActionCard
					title="Usuários Inativos"
					description="Gerenciar usuários inativos"
					action={() => openInactiveUsersModal()}
					icon="⏰"
				/>
			</div>
		</div>
	);
};
```

### Cenário 4: Criação de Admin Secundário

#### Processo Restrito ao Admin Primário

```bash
# Apenas admin primário pode criar outros admins
POST /api/admin/secondary-admins
Authorization: Bearer {primary-admin-token}
{
  "email": "admin.regional@techmel.com",
  "name": "Ana Admin Regional São Paulo"
}

# Validação automática:
# 1. Token pertence a admin primário?
# 2. Email é único?
# 3. Usuário tem privilégios?

# Se válido, sistema:
# 1. Cria usuário com role ADMIN, isPrimary = false
# 2. Gera senha temporária
# 3. Envia email com credenciais
# 4. Registra na auditoria
# 5. Retorna dados do admin criado

HTTP 201 Created
{
  "id": "admin-secondary-uuid",
  "email": "admin.regional@techmel.com",
  "name": "Ana Admin Regional São Paulo",
  "role": "ADMIN",
  "isPrimary": false,
  "requiresPasswordChange": true
}
```

#### Log de Auditoria Gerado

```json
{
	"action": "CREATE",
	"entityType": "USER",
	"entityId": "admin-secondary-uuid",
	"details": "Administrador secundário criado: admin.regional@techmel.com",
	"userId": "primary-admin-uuid",
	"userName": "Admin Principal Sistema",
	"timestamp": "2025-01-15T16:45:00"
}
```

---

Esta documentação cobre todos os aspectos do módulo de Administração no TechMel. O sistema é projetado com foco em segurança, auditabilidade e usabilidade, fornecendo aos administradores ferramentas robustas para gestão completa do sistema e seus usuários.
