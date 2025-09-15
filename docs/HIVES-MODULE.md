# Documentação Técnica: Módulo de Gerenciamento de Colmeias (Hives) no TechMel

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura da Solução](#2-arquitetura-da-solução)
3. [Entidades e Relacionamentos](#3-entidades-e-relacionamentos)
4. [Casos de Uso (Use Cases)](#4-casos-de-uso-use-cases)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [Integração entre Módulos](#6-integração-entre-módulos)
7. [Implementação das APIs](#7-implementação-das-apis)
8. [Controle de Acesso e Segurança](#8-controle-de-acesso-e-segurança)
9. [Gestão do Ciclo de Vida](#9-gestão-do-ciclo-de-vida)
10. [Exemplos de Interação](#10-exemplos-de-interação)

## 1. Visão Geral

O módulo de Colmeias (Hives) é o **núcleo operacional** do TechMel, responsável pelo gerenciamento completo das colmeias inteligentes desde a criação até o monitoramento. Este sistema permite que usuários adquiram colmeias virtuais e que técnicos as implementem fisicamente, criando uma ponte entre o mundo digital e os dispositivos IoT no campo.

### Características Principais

- **Gestão Completa**: Criação, atualização, ativação e exclusão de colmeias
- **Controle de Propriedade**: Sistema de ownership com validação de permissões
- **Integração IoT**: Geração automática de chaves de API para dispositivos
- **Status Management**: Controle de status ACTIVE/INACTIVE
- **Disponibilidade**: Sistema de colmeias disponíveis por usuário
- **Multi-Role**: Diferentes funcionalidades para técnicos e usuários comuns
- **Rastreabilidade**: Auditoria completa de todas as operações

### O que são Hives no TechMel?

Hives (colmeias) são representações digitais de colmeias físicas equipadas com sensores IoT. Cada hive possui:

- **Identificação única**: UUID gerado automaticamente
- **Chave de API**: Para comunicação com dispositivos IoT
- **Status de atividade**: INACTIVE (inativa) ou ACTIVE (ativa)
- **Localização**: Endereço físico da colmeia
- **Proprietário**: Usuário que adquiriu a colmeia
- **Metadados**: Timestamps de criação e atualização

### Papéis de Usuário no Sistema

```
┌─────────────────────┐
│ TÉCNICO             │ ← Criação e gestão técnica das colmeias
│ (ROLE_TECHNICIAN)   │   Acesso total para configuração
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ USUÁRIO COMUM       │ ← Visualização e acompanhamento
│ (ROLE_COMMON)       │   Acesso apenas às suas colmeias
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ ADMINISTRADOR       │ ← Controle total do sistema
│ (ROLE_ADMIN)        │   Herda privilégios de técnico
└─────────────────────┘
```

## 2. Arquitetura da Solução

### Diagrama de Componentes

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   PostgreSQL    │
│   (React/Vue)   │    │   (Spring)      │    │   Database      │
│                 │    │                 │    │                 │
│ • Dashboard     │◄──►│ • HiveController│◄──►│ • hives         │
│ • Hive List     │    │ • HiveService   │    │ • users         │
│ • Hive Details  │    │ • Security      │    │ • measurements  │
│ • Management    │    │ • Validation    │    │ • alerts        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   IoT Devices   │
                       │   (Via API Key) │
                       │                 │
                       │ • Sensors       │
                       │ • Data Sending  │
                       │ • Authentication│
                       └─────────────────┘
```

### Componentes Principais

1. **HiveController**: Endpoints REST para gerenciamento de colmeias
2. **HiveService**: Lógica de negócio e validações
3. **HiveRepositoryPort**: Interface para persistência de dados
4. **SecurityConfig**: Controle de acesso baseado em roles
5. **HiveEntity**: Entidade JPA para mapeamento da tabela
6. **DTOs**: Objetos de transferência de dados (Request/Response)
7. **AuthenticationUtil**: Controle de identidade e permissões

## 3. Entidades e Relacionamentos

### Entidade Hive

```java
public class Hive {
    private UUID id;                    // Identificador único
    private String name;                // Nome da colmeia
    private String location;            // Localização física
    private String apiKey;              // Chave para dispositivos IoT
    private HiveStatus hiveStatus;      // INACTIVE ou ACTIVE
    private LocalDateTime createdAt;    // Data de criação
    private LocalDateTime updatedAt;    // Data de atualização
    private User owner;                 // Proprietário da colmeia
}

public enum HiveStatus {
    INACTIVE,   // Colmeia criada mas não operacional
    ACTIVE      // Colmeia operacional e recebendo dados
}
```

### Entidade User (campos relevantes)

```java
public class User {
    private UUID id;
    private String name;
    private String email;
    private Role role;                  // ADMIN, TECHNICIAN, COMMON
    private Integer availableHives;     // Colmeias disponíveis para criação
    // ... outros campos
}
```

### Relacionamentos

- **Hive ↔ User**: Relacionamento Many-to-One (várias colmeias para um usuário)
- **Cascade Delete**: Quando um usuário é deletado, suas colmeias também são removidas
- **Índices**: Otimização para consultas por owner_id
- **Unique Constraints**: API Key única por colmeia

### Estados e Lifecycle

```
┌─────────────┐    CREATE     ┌─────────────┐    ACTIVATE   ┌─────────────┐
│   PENDING   │ ──────────────► │  INACTIVE   │ ──────────────► │   ACTIVE    │
│   (Virtual) │               │ (Physical)  │               │ (Operational)│
└─────────────┘               └─────────────┘               └─────────────┘
                                      │                             │
                                  DELETE │                    DEACTIVATE │
                                      ▼                             ▼
                              ┌─────────────┐                ┌─────────────┐
                              │   DELETED   │                │  INACTIVE   │
                              │ (+1 avail.) │                │ (Maintenance)│
                              └─────────────┘                └─────────────┘
```

## 4. Casos de Uso (Use Cases)

### Interface HiveUseCase

```java
public interface HiveUseCase {
    // Operações de CRUD
    Hive createHive(CreateHiveRequest request);
    Hive getHiveById(UUID hiveId, UUID ownerId);
    void updateApiKey(UUID hiveId, String newApiKey);
    void updateHiveStatus(UUID hiveId, HiveStatus hiveStatus);
    void deleteHive(UUID hiveId);

    // Consultas e Listagens
    Page<Hive> listHivesByOwner(UUID ownerId, Pageable pageable);
    Page<Hive> listAllHives(Pageable pageable);
    Page<User> listAllUsersWithAvailableHives(Pageable pageable);
}
```

### Casos de Uso Detalhados

#### UC01: Criação de Colmeia

- **Ator**: Técnico
- **Pré-condição**: Usuário com role TECHNICIAN
- **Fluxo Principal**:
  1. Técnico identifica usuário com colmeias disponíveis
  2. Técnico fornece nome, localização e ID do proprietário
  3. Sistema valida existência do usuário
  4. Sistema verifica `availableHives > 0`
  5. Sistema cria colmeia com status INACTIVE
  6. Sistema gera UUID e chave de API únicos
  7. Sistema decrementa `availableHives` do usuário
  8. Retorna dados da colmeia criada

#### UC02: Listagem de Colmeias por Usuário

- **Ator**: Usuário autenticado
- **Pré-condição**: Usuário logado no sistema
- **Fluxo Principal**:
  1. Sistema identifica usuário através do token JWT
  2. Sistema busca todas as colmeias do usuário
  3. Sistema retorna dados paginados (sem chave de API para segurança)
  4. Usuário visualiza suas colmeias no dashboard

#### UC03: Gestão Técnica de Colmeias

- **Ator**: Técnico
- **Pré-condição**: Usuário com role TECHNICIAN
- **Operações Disponíveis**:
  - Listar todas as colmeias do sistema
  - Ativar/desativar colmeias
  - Atualizar chaves de API
  - Remover colmeias do sistema
  - Visualizar usuários com colmeias disponíveis

#### UC04: Monitoramento de Disponibilidade

- **Ator**: Técnico
- **Pré-condição**: Necessidade de criar novas colmeias
- **Fluxo Principal**:
  1. Técnico consulta usuários com `availableHives > 0`
  2. Sistema retorna lista paginada de usuários elegíveis
  3. Técnico seleciona usuário para criação de colmeia
  4. Processo de criação é iniciado

#### UC05: Ativação de Colmeia

- **Ator**: Técnico
- **Pré-condição**: Colmeia física instalada e configurada
- **Fluxo Principal**:
  1. Técnico acessa colmeia com status INACTIVE
  2. Técnico configura dispositivos IoT com a chave de API
  3. Técnico atualiza status para ACTIVE
  4. Sistema habilita recepção de dados dos sensores
  5. Colmeia fica operacional para monitoramento

## 5. Fluxo de Dados

### Fluxo de Criação de Colmeia

```
1. Dashboard Técnico
   │ POST /api/technician/hives
   │ Body: {name, location, ownerId}
   ▼
2. HiveController (@PreAuthorize TECHNICIAN)
   │ @Valid CreateHiveRequest
   │ Security check and validation
   ▼
3. HiveService.createHive()
   │ ├─ userRepositoryPort.findById(ownerId)
   │ ├─ validate user.getAvailableHives() > 0
   │ ├─ generate UUID and API key
   │ ├─ build Hive with INACTIVE status
   │ ├─ hiveRepositoryPort.save(hive)
   │ ├─ decrement user.availableHives
   │ └─ userRepositoryPort.save(user)
   ▼
4. PostgreSQL Transaction
   │ INSERT INTO hives (id, name, location, api_key, status, owner_id, ...)
   │ UPDATE users SET available_hives = available_hives - 1 WHERE id = ?
   ▼
5. Response HTTP 201
   │ HiveResponse {id, name, location, apiKey, status, ownerId}
```

### Fluxo de Listagem para Usuário

```
1. User Dashboard
   │ GET /api/my/hives?page=0&size=10
   │ Headers: Authorization: Bearer {jwt}
   ▼
2. HiveController (any authenticated user)
   │ authenticationUtil.getCurrentUserId()
   │ Pageable parameters
   ▼
3. HiveService.listHivesByOwner()
   │ ├─ userRepositoryPort.findById(userId) // validate existence
   │ └─ hiveRepositoryPort.findByOwnerId(userId, pageable)
   ▼
4. Security Filtering
   │ Query: SELECT * FROM hives WHERE owner_id = ? ORDER BY created_at DESC
   │ Only user's own hives are returned
   ▼
5. Response Mapping
   │ Page<Hive> → Page<GetMyHivesResponse>
   │ apiKey field is excluded for security
   ▼
6. JSON Response
   │ Paginated list without sensitive data
```

### Fluxo de Ativação de Colmeia

```
1. Technician Action
   │ PATCH /api/technician/hive-status/{hiveId}
   │ Body: {hiveStatus: "ACTIVE"}
   ▼
2. Security and Validation
   │ @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
   │ @Valid UpdateHiveStatusRequest
   ▼
3. HiveService.updateHiveStatus()
   │ ├─ hiveRepositoryPort.findById(hiveId)
   │ ├─ validate hive exists
   │ ├─ update hive.setHiveStatus(ACTIVE)
   │ └─ hiveRepositoryPort.save(hive)
   ▼
4. Database Update
   │ UPDATE hives SET hive_status = 'ACTIVE', updated_at = NOW()
   │ WHERE id = ?
   ▼
5. System Integration
   │ Hive now accepts IoT measurements
   │ MeasurementService can process data from this hive
   ▼
6. Response HTTP 204
   │ No content - operation successful
```

## 6. Integração entre Módulos

### Hive → User (Gerenciamento de Disponibilidade)

```java
// Criação de colmeia decrementa disponibilidade
owner.setAvailableHives(owner.getAvailableHives() - 1);
userRepositoryPort.save(owner);

// Deleção de colmeia restaura disponibilidade
owner.setAvailableHives(owner.getAvailableHives() + 1);
userRepositoryPort.save(owner);
```

**Características da Integração**:

- **Transacional**: Operações atômicas garantem consistência
- **Bidirecional**: Criação decrementa, deleção incrementa
- **Validação**: Previne criação sem disponibilidade
- **Auditoria**: Mudanças são logadas pelo sistema

### Hive → Measurement (Via API Key)

```java
// MeasurementService valida colmeia através da API key
Hive hive = hiveRepositoryPort.findByApiKey(apiKey)
    .orElseThrow(() -> new NotFoundException("Hive not found for API key"));

if (hive.getHiveStatus() == Hive.HiveStatus.INACTIVE) {
    throw new ConflictException("Cannot register measurement for inactive hive");
}
```

**Relacionamento**:

- **Autenticação**: API Key é o mecanismo de segurança IoT
- **Status**: Só colmeias ACTIVE podem receber medições
- **Rastreabilidade**: Cada medição é vinculada a uma colmeia específica

### Hive → Alert (Através de Measurements)

```java
// Fluxo: Measurement → Hive validation → Alert generation
// Alertas são criados apenas para colmeias ativas
if (hive.getHiveStatus() == HiveStatus.ACTIVE) {
    alertService.checkThresholds(measurement, hive);
}
```

**Integração Indireta**:

- Alertas dependem de medições válidas
- Medições dependem de colmeias ativas
- Hives controlam o fluxo de dados do sistema

### Hive → Threshold (Configuração de Limites)

```java
// Cada colmeia pode ter seus próprios limites configurados
Optional<Threshold> threshold = thresholdRepository.findByHiveId(hive.getId());
```

**Relacionamento 1:1**:

- Uma colmeia pode ter um threshold configurado
- Threshold é opcional mas recomendado
- Deletion cascade: remover hive remove threshold

## 7. Implementação das APIs

### Endpoints Principais

#### POST /api/technician/hives

**Criar nova colmeia (Técnicos)**

```bash
curl -X POST http://localhost:8080/api/technician/hives \
  -H "Authorization: Bearer {technician-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Colmeia Principal",
    "location": "Apiário Norte - Setor A1",
    "ownerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }'
```

**Validações**:

- Nome: 3-100 caracteres, obrigatório
- Localização: 3-255 caracteres, obrigatória
- OwnerId: Deve existir e ter `availableHives > 0`
- Usuário: Deve ter role TECHNICIAN

**Resposta**:

```json
{
	"id": "hive-uuid-123",
	"name": "Colmeia Principal",
	"location": "Apiário Norte - Setor A1",
	"apiKey": "hive_key_abc123def456",
	"hiveStatus": "INACTIVE",
	"ownerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### GET /api/my/hives

**Listar minhas colmeias (Usuários)**

```bash
curl -X GET \
  'http://localhost:8080/api/my/hives?page=0&size=10' \
  -H 'Authorization: Bearer {user-token}'
```

**Resposta**:

```json
{
	"content": [
		{
			"id": "hive-uuid-123",
			"name": "Colmeia Principal",
			"location": "Apiário Norte - Setor A1",
			"hiveStatus": "ACTIVE",
			"ownerId": "user-uuid-456"
		}
	],
	"totalElements": 5,
	"totalPages": 1,
	"size": 10,
	"number": 0
}
```

**Nota**: API Key é omitida por segurança para usuários comuns.

#### GET /api/technician/hives

**Listar todas as colmeias (Técnicos)**

```bash
curl -X GET \
  'http://localhost:8080/api/technician/hives?page=0&size=20' \
  -H 'Authorization: Bearer {technician-token}'
```

**Resposta inclui API Key** para configuração de dispositivos.

#### GET /api/technician/available-users

**Usuários com colmeias disponíveis**

```bash
curl -X GET \
  'http://localhost:8080/api/technician/available-users' \
  -H 'Authorization: Bearer {technician-token}'
```

**Resposta**:

```json
{
	"content": [
		{
			"id": "user-uuid-123",
			"name": "João Silva",
			"email": "joao@exemplo.com",
			"availableHives": 3
		}
	]
}
```

#### PATCH /api/technician/hive-status/{hiveId}

**Atualizar status da colmeia**

```bash
curl -X PATCH \
  'http://localhost:8080/api/technician/hive-status/hive-uuid-123' \
  -H 'Authorization: Bearer {technician-token}' \
  -H 'Content-Type: application/json' \
  -d '{"hiveStatus": "ACTIVE"}'
```

#### DELETE /api/technician/hives/{hiveId}

**Deletar colmeia**

```bash
curl -X DELETE \
  'http://localhost:8080/api/technician/hives/hive-uuid-123' \
  -H 'Authorization: Bearer {technician-token}'
```

**Efeito**: Incrementa `availableHives` do proprietário.

### Códigos de Resposta

- **200 OK**: Consulta realizada com sucesso
- **201 Created**: Colmeia criada com sucesso
- **204 No Content**: Operação de atualização/deleção concluída
- **400 Bad Request**: Dados inválidos ou usuário sem colmeias disponíveis
- **401 Unauthorized**: Token inválido ou expirado
- **403 Forbidden**: Privilégios insuficientes ou acesso negado à colmeia
- **404 Not Found**: Colmeia, usuário não encontrados

## 8. Controle de Acesso e Segurança

### Matriz de Permissões

| Operação                 | Técnico | Usuário Comum | Admin |
| ------------------------ | ------- | ------------- | ----- |
| Criar Colmeia            | ✅      | ❌            | ✅    |
| Listar Todas Colmeias    | ✅      | ❌            | ✅    |
| Listar Minhas Colmeias   | ✅      | ✅            | ✅    |
| Ver Detalhes Específicos | ✅¹     | ✅²           | ✅    |
| Atualizar Status         | ✅      | ❌            | ✅    |
| Atualizar API Key        | ✅      | ❌            | ✅    |
| Deletar Colmeia          | ✅      | ❌            | ✅    |
| Ver Usuários Disponíveis | ✅      | ❌            | ✅    |

¹ Técnicos veem todas as colmeias com API Key
² Usuários veem apenas suas colmeias sem API Key

### Validação de Propriedade

```java
// Usuários comuns só acessam suas próprias colmeias
public Hive getHiveById(UUID hiveId, UUID ownerId) {
    Hive hive = hiveRepositoryPort.findById(hiveId)
        .orElseThrow(() -> new NotFoundException("Hive não encontrada"));

    if (!hive.getOwner().getId().equals(ownerId)) {
        throw new ForbiddenException("Você não tem permissão para acessar esta colmeia.");
    }

    return hive;
}
```

### Proteção de Dados Sensíveis

```java
// API Key é omitida para usuários comuns
Page<GetMyHivesResponse> response = page.map(hive -> GetMyHivesResponse.builder()
    .id(hive.getId())
    .name(hive.getName())
    .location(hive.getLocation())
    .hiveStatus(hive.getHiveStatus())
    .ownerId(hive.getOwner().getId())
    // API Key não incluída por segurança
    .build()
);
```

### Geração Segura de API Keys

```java
// API Keys são UUIDs únicos
Hive hive = Hive.builder()
    .apiKey(UUID.randomUUID().toString())
    // ... outros campos
    .build();
```

**Características**:

- **UUID v4**: Geração aleatória criptograficamente segura
- **Uniqueness**: Garantida pelo UUID
- **Rotation**: Técnicos podem atualizar quando necessário
- **Validation**: Verificada em cada medição IoT

## 9. Gestão do Ciclo de Vida

### Fases do Ciclo de Vida

#### Fase 1: Aquisição (Simulada)

```
┌─────────────┐     Compra        ┌───────────────┐
│   Usuário   │ ─────────────────► │   Sistema     │
│             │ (processo externo) │ (incrementa   │
│             │                   │ availableHives)│
└─────────────┘                   └───────────────┘
```

**Estado**: Usuário com `availableHives > 0`

#### Fase 2: Criação Técnica

```
┌─────────────┐     Identifica     ┌───────────────┐
│   Técnico   │ ──────────────────► │ Usuário com   │
│             │ usuários elegíveis │ Hives Disponív│
│             │ ◄──────────────────┤               │
└─────────────┘                    └───────────────┘
       │
       ▼ Cria Colmeia
┌─────────────┐
│ Hive INACTIVE│
│ API Key gen. │
│ avail. hives-│
└─────────────┘
```

**Estado**: Colmeia criada, status INACTIVE

#### Fase 3: Implementação Física

```
┌─────────────┐    Instala         ┌───────────────┐
│   Técnico   │ ──────────────────► │ Dispositivos  │
│             │ dispositivos IoT   │ IoT no campo  │
│             │                    │               │
└─────────────┘                    └───────────────┘
       │
       ▼ Configura com API Key
┌─────────────┐
│ Devices     │
│ Configured  │
│ with API Key│
└─────────────┘
```

**Estado**: Hardware instalado e configurado

#### Fase 4: Ativação

```
┌─────────────┐    Atualiza Status ┌───────────────┐
│   Técnico   │ ──────────────────► │ Hive ACTIVE   │
│             │   para ACTIVE      │               │
│             │                    │               │
└─────────────┘                    └───────────────┘
       │
       ▼ Sistema aceita dados
┌─────────────┐
│ IoT Data    │
│ Flowing     │
│ to System   │
└─────────────┘
```

**Estado**: Operacional, recebendo dados

#### Fase 5: Operação e Monitoramento

```
┌─────────────┐     Visualiza      ┌───────────────┐
│   Usuário   │ ◄─────────────────►│ Dashboard     │
│ Proprietário│   suas colmeias    │ Medições      │
│             │                    │ Alertas       │
└─────────────┘                    └───────────────┘
       ▲
       │ Dados dos sensores
       ▼
┌─────────────┐
│ IoT Devices │
│ Sending     │
│ Measurements│
└─────────────┘
```

**Estado**: Totalmente operacional

### Transições de Status

```java
// Status pode ser alterado apenas por técnicos
@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
public void updateHiveStatus(UUID hiveId, HiveStatus hiveStatus) {
    Hive hive = hiveRepositoryPort.findById(hiveId)
        .orElseThrow(() -> new NotFoundException("Hive não encontrada"));

    hive.setHiveStatus(hiveStatus);
    hiveRepositoryPort.save(hive);
}
```

**Regras de Transição**:

- `INACTIVE → ACTIVE`: Quando hardware está configurado
- `ACTIVE → INACTIVE`: Para manutenção ou problemas
- Não há status intermediários
- Mudanças são logadas para auditoria

## 10. Exemplos de Interação

### Cenário 1: Criação Completa de Colmeia

#### Passo 1: Técnico Identifica Oportunidade

```bash
# Técnico busca usuários com colmeias disponíveis
GET /api/technician/available-users
Authorization: Bearer {technician-token}

# Resposta mostra usuários elegíveis:
{
  "content": [
    {
      "id": "user-uuid-123",
      "name": "Carlos Apicultor",
      "email": "carlos@email.com",
      "availableHives": 2
    }
  ]
}
```

#### Passo 2: Criação da Colmeia

```bash
# Técnico cria colmeia para o usuário
POST /api/technician/hives
Authorization: Bearer {technician-token}
{
  "name": "Colmeia Carlos - Norte",
  "location": "Propriedade Rural - Setor Norte, Coordenadas: -23.5505, -46.6333",
  "ownerId": "user-uuid-123"
}

# Sistema responde:
HTTP 201 Created
{
  "id": "hive-uuid-new",
  "name": "Colmeia Carlos - Norte",
  "location": "Propriedade Rural - Setor Norte, Coordenadas: -23.5505, -46.6333",
  "apiKey": "550e8400-e29b-41d4-a716-446655440000",
  "hiveStatus": "INACTIVE",
  "ownerId": "user-uuid-123"
}
```

#### Passo 3: Instalação Física

```
Técnico vai ao campo com:
1. API Key: 550e8400-e29b-41d4-a716-446655440000
2. Localização: Propriedade Rural - Setor Norte
3. Dispositivos IoT (sensores, transmissor)

Processo no campo:
1. Instalar sensores na colmeia física
2. Configurar transmissor com API Key
3. Testar conectividade
4. Verificar envio de dados de teste
```

#### Passo 4: Ativação da Colmeia

```bash
# Após instalação bem-sucedida
PATCH /api/technician/hive-status/hive-uuid-new
Authorization: Bearer {technician-token}
{
  "hiveStatus": "ACTIVE"
}

# Resposta:
HTTP 204 No Content

# Sistema agora aceita medições desta colmeia
```

#### Passo 5: Usuário Visualiza Sua Nova Colmeia

```bash
# Carlos (proprietário) acessa seu dashboard
GET /api/my/hives
Authorization: Bearer {carlos-token}

# Vê sua nova colmeia:
{
  "content": [
    {
      "id": "hive-uuid-new",
      "name": "Colmeia Carlos - Norte",
      "location": "Propriedade Rural - Setor Norte, Coordenadas: -23.5505, -46.6333",
      "hiveStatus": "ACTIVE",
      "ownerId": "user-uuid-123"
      // Nota: API Key não é mostrada por segurança
    }
  ]
}
```

### Cenário 2: Manutenção de Colmeia

#### Situação: Problema Técnico Detectado

```bash
# Alertas mostram problema na colmeia
# Técnico decide desativar temporariamente

PATCH /api/technician/hive-status/hive-uuid-problem
Authorization: Bearer {technician-token}
{
  "hiveStatus": "INACTIVE"
}

# Resultado:
# - Colmeia para de aceitar medições
# - Usuário vê status INACTIVE no dashboard
# - Alertas param de ser gerados
```

#### Resolução e Reativação

```bash
# Após reparo físico
PATCH /api/technician/hive-status/hive-uuid-problem
{
  "hiveStatus": "ACTIVE"
}

# Colmeia volta a funcionar normalmente
```

### Cenário 3: Remoção de Colmeia

#### Processo de Desmontagem

```bash
# Técnico decide remover colmeia
DELETE /api/technician/hives/hive-uuid-old
Authorization: Bearer {technician-token}

# Sistema:
# 1. Remove colmeia do banco
# 2. Incrementa availableHives do proprietário
# 3. Remove dados relacionados (cascade)

HTTP 204 No Content

# Usuário ganha crédito para nova colmeia
```

### Cenário 4: Dashboard do Usuário

#### Implementação Frontend

```typescript
interface UserHivesProps {}

const UserHivesDashboard: React.FC<UserHivesProps> = () => {
	const [hives, setHives] = useState<Hive[]>([]);
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		const fetchHives = async () => {
			try {
				const response = await api.get("/my/hives");
				setHives(response.data.content);
			} catch (error) {
				toast.error("Erro ao carregar suas colmeias");
			} finally {
				setLoading(false);
			}
		};

		fetchHives();
	}, []);

	if (loading) return <LoadingSpinner />;

	return (
		<div className="user-hives-dashboard">
			<h1>Minhas Colmeias</h1>

			{hives.length === 0 ? (
				<EmptyState
					message="Você ainda não possui colmeias"
					description="Entre em contato com nossa equipe para adquirir suas primeiras colmeias inteligentes"
					action={
						<button onClick={() => openContactModal()}>
							Solicitar Colmeias
						</button>
					}
				/>
			) : (
				<div className="hives-grid">
					{hives.map((hive) => (
						<HiveCard key={hive.id}>
							<div className="hive-header">
								<h3>{hive.name}</h3>
								<StatusBadge status={hive.hiveStatus} />
							</div>

							<div className="hive-location">
								<Icon name="location" />
								<span>{hive.location}</span>
							</div>

							<div className="hive-actions">
								<button
									onClick={() => viewHiveDetails(hive.id)}
									className="primary"
								>
									Ver Detalhes
								</button>
								<button
									onClick={() => viewMeasurements(hive.id)}
									disabled={hive.hiveStatus === "INACTIVE"}
								>
									Medições
								</button>
								<button
									onClick={() => viewAlerts(hive.id)}
									disabled={hive.hiveStatus === "INACTIVE"}
								>
									Alertas
								</button>
							</div>

							{hive.hiveStatus === "ACTIVE" && (
								<div className="quick-stats">
									<QuickMeasurements hiveId={hive.id} />
								</div>
							)}
						</HiveCard>
					))}
				</div>
			)}
		</div>
	);
};

const StatusBadge: React.FC<{ status: HiveStatus }> = ({ status }) => {
	const getStatusConfig = (status: HiveStatus) => {
		switch (status) {
			case "ACTIVE":
				return { color: "green", icon: "✅", text: "Ativa" };
			case "INACTIVE":
				return { color: "orange", icon: "⏸️", text: "Inativa" };
			default:
				return { color: "gray", icon: "❓", text: "Desconhecido" };
		}
	};

	const config = getStatusConfig(status);

	return (
		<span className={`status-badge status-${config.color}`}>
			{config.icon} {config.text}
		</span>
	);
};
```

### Cenário 5: Dashboard Técnico

```typescript
const TechnicianDashboard: React.FC = () => {
	const [allHives, setAllHives] = useState<Hive[]>([]);
	const [availableUsers, setAvailableUsers] = useState<User[]>([]);
	const [selectedUser, setSelectedUser] = useState<User | null>(null);

	const createHiveForUser = async (userData: CreateHiveData) => {
		try {
			const response = await api.post("/technician/hives", {
				name: userData.name,
				location: userData.location,
				ownerId: selectedUser?.id,
			});

			toast.success(`Colmeia "${userData.name}" criada com sucesso!`);
			toast.info(`API Key: ${response.data.apiKey}`);

			// Refresh data
			fetchAvailableUsers();
			fetchAllHives();
		} catch (error) {
			toast.error("Erro ao criar colmeia");
		}
	};

	return (
		<div className="technician-dashboard">
			<h1>Dashboard Técnico</h1>

			<div className="dashboard-sections">
				{/* Seção de Criação */}
				<section className="create-hive-section">
					<h2>Criar Nova Colmeia</h2>

					<UserSelector
						users={availableUsers}
						selectedUser={selectedUser}
						onUserSelect={setSelectedUser}
					/>

					{selectedUser && (
						<CreateHiveForm user={selectedUser} onSubmit={createHiveForUser} />
					)}
				</section>

				{/* Seção de Gerenciamento */}
				<section className="manage-hives-section">
					<h2>Gerenciar Colmeias</h2>

					<HiveManagementTable
						hives={allHives}
						onStatusChange={updateHiveStatus}
						onApiKeyUpdate={updateApiKey}
						onDelete={deleteHive}
					/>
				</section>
			</div>
		</div>
	);
};
```

---

Esta documentação cobre todos os aspectos do módulo de Hives no TechMel. O sistema é projetado para ser robusto, seguro e escalável, fornecendo uma ponte eficiente entre o mundo digital e os dispositivos IoT no campo, sempre mantendo a segurança e a experiência do usuário como prioridades.
