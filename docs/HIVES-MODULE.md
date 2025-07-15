# Documentação Técnica: Módulo de Gerenciamento de Hives (Colmeias) no TechMel

## Sumário

1. Introdução
2. Arquitetura da Solução
3. Modelo de Dados
4. Funcionalidades por Tipo de Usuário
5. Fluxo Completo de Uso
6. Implementação das APIs
7. Gerenciamento Manual via PostgreSQL
8. Boas Práticas de Segurança
9. Troubleshooting
10. Referências

## 1. Introdução

O módulo de Hives do TechMel é responsável pelo gerenciamento completo das colmeias inteligentes, desde a criação até o monitoramento. Este sistema permite que usuários adquiram colmeias virtuais e que técnicos as implementem fisicamente, criando uma ponte entre o mundo digital e os dispositivos IoT no campo.

### O que são Hives no TechMel?

Hives (colmeias) são representações digitais de colmeias físicas equipadas com sensores IoT. Cada hive possui:

- **Identificação única**: UUID gerado automaticamente
- **Chave de API**: Para comunicação com dispositivos IoT
- **Status de atividade**: INACTIVE (inativa) ou ACTIVE (ativa)
- **Localização**: Endereço físico da colmeia
- **Proprietário**: Usuário que adquiriu a colmeia
- **Metadados**: Timestamps de criação e atualização

### Papéis de Usuário no Sistema

- **COMMON**: Usuários regulares que podem adquirir e visualizar suas colmeias
- **TECHNICIAN**: Técnicos responsáveis pela criação e gerenciamento físico das colmeias
- **ADMIN**: Administradores com acesso total ao sistema

## 2. Arquitetura da Solução

### Diagrama de Componentes

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   PostgreSQL    │
│   (React/Vue)   │    │   (Spring)      │    │   Database      │
│                 │    │                 │    │                 │
│ • Dashboard     │◄──►│ • HiveController│◄──►│ • users         │
│ • Hive List     │    │ • HiveService   │    │ • hives         │
│ • Hive Details  │    │ • Security      │    │ • refresh_tokens│
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   IoT Devices   │
                       │   (Via API Key) │
                       └─────────────────┘
```

### Componentes Principais

1. **HiveController**: Endpoints REST para gerenciamento de colmeias
2. **HiveService**: Lógica de negócio e validações
3. **HiveRepositoryPort**: Interface para persistência de dados
4. **SecurityConfig**: Controle de acesso baseado em roles
5. **HiveEntity**: Entidade JPA para mapeamento da tabela
6. **DTOs**: Objetos de transferência de dados (Request/Response)

## 3. Modelo de Dados

### Entidade Hive

```java
@Entity
@Table(name = "hives")
public class HiveEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false)
    private String location;
    
    @Column(nullable = false, name = "api_key")
    private String apiKey;
    
    @Column(nullable = false, name = "hive_status")
    @Enumerated(EnumType.STRING)
    private HiveStatus hiveStatus;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity owner;
}
```

### Entidade User (campos relevantes)

```java
@Entity
@Table(name = "users")
public class UserEntity {
    // ... outros campos ...
    
    @Column(name = "available_hives", nullable = false)
    private int availableHives;
    
    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN, TECHNICIAN, COMMON
}
```

### Relacionamentos

- **Hive ↔ User**: Relacionamento Many-to-One (várias colmeias para um usuário)
- **Cascade Delete**: Quando um usuário é deletado, suas colmeias também são removidas
- **Índices**: Otimização para consultas por owner_id

## 4. Funcionalidades por Tipo de Usuário

### Usuários COMMON

#### Visualizar Minhas Colmeias
- **Endpoint**: `GET /api/my/hives`
- **Permissão**: Qualquer usuário autenticado
- **Funcionalidade**: Lista todas as colmeias do usuário logado
- **Resposta**: Dados da colmeia SEM a chave de API (segurança)

#### Visualizar Detalhes de uma Colmeia
- **Endpoint**: `GET /api/my/hives/{hiveId}`
- **Permissão**: Apenas o proprietário da colmeia
- **Funcionalidade**: Retorna detalhes completos de uma colmeia específica
- **Validação**: Verifica se a colmeia pertence ao usuário autenticado

### Usuários TECHNICIAN

#### Listar Usuários com Colmeias Disponíveis
- **Endpoint**: `GET /api/technician/available-users`
- **Permissão**: `@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")`
- **Funcionalidade**: Lista usuários que possuem `availableHives > 0`
- **Uso**: Identificar para quem criar novas colmeias

#### Criar Nova Colmeia
- **Endpoint**: `POST /api/technician/hives`
- **Permissão**: `@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")`
- **Funcionalidade**: Cria uma nova colmeia para um usuário específico
- **Validações**:
  - Usuário deve existir
  - Usuário deve ter `availableHives > 0`
  - Campos obrigatórios: name, location, ownerId

#### Listar Todas as Colmeias
- **Endpoint**: `GET /api/technician/hives`
- **Permissão**: `@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")`
- **Funcionalidade**: Lista todas as colmeias do sistema
- **Resposta**: Inclui chave de API para configuração de dispositivos

#### Atualizar Chave de API
- **Endpoint**: `PATCH /api/technician/hives/{hiveId}/api-key`
- **Permissão**: `@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")`
- **Funcionalidade**: Atualiza a chave de API de uma colmeia
- **Uso**: Reconfiguração de dispositivos IoT

#### Atualizar Status da Colmeia
- **Endpoint**: `PATCH /api/technician/hives/{hiveId}/status`
- **Permissão**: `@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")`
- **Funcionalidade**: Altera status entre ACTIVE/INACTIVE
- **Uso**: Ativação/desativação de colmeias

#### Deletar Colmeia
- **Endpoint**: `DELETE /api/technician/hives/{hiveId}`
- **Permissão**: `@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")`
- **Funcionalidade**: Remove uma colmeia do sistema
- **Efeito**: Incrementa `availableHives` do proprietário

### Usuários ADMIN

Herdam todas as permissões de TECHNICIAN, podendo gerenciar todo o sistema.

## 5. Fluxo Completo de Uso

### Fase 1: Aquisição de Colmeias (Simulada)

```
┌─────────────┐     1. Compra Hive      ┌───────────────┐
│             ├────────────────────────>│               │
│   Usuário   │                         │   Sistema     │
│   COMMON    │                         │   (Manual)    │
│             │<────────────────────────┤               │
└─────────────┘  2. availableHives++    └───────────────┘
```

**Processo atual (manual - TEMPORÁRIO)**:
1. Usuário solicita compra de colmeias (processo externo - EM DESENVOLVIMENTO)
2. Sistema incrementa campo `available_hives` via SQL (ARTIFÍCIO TEMPORÁRIO)
3. Usuário pode verificar suas colmeias disponíveis no dashboard

> 🚧 **Em Desenvolvimento**: O sistema completo de compras incluirá integração com gateway de pagamento, notificações automáticas e dashboard administrativo.

### Fase 2: Identificação de Oportunidades

```
┌─────────────┐     1. Lista usuários    ┌───────────────┐
│             ├────────────────────────> │               │
│  Técnico    │     com hives disponíveis│   Sistema     │
│ TECHNICIAN  │                          │               │
│             │ <────────────────────────┤               │
└─────────────┘   2. Retorna lista       └───────────────┘
```

**Endpoint**: `GET /api/technician/available-users`

**Resposta típica**:
```json
{
  "content": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "name": "João Silva",
      "email": "joao@exemplo.com",
      "availableHives": 3
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

### Fase 3: Criação da Colmeia

```
┌─────────────┐     1. Cria hive        ┌───────────────┐
│             ├────────────────────────>│               │
│  Técnico    │     para usuário        │   Sistema     │
│ TECHNICIAN  │                         │               │
│             │<────────────────────────┤               │
└─────────────┘  2. Hive criada +       └───────────────┘
                    availableHives--
```

**Endpoint**: `POST /api/technician/hives`

**Payload**:
```json
{
  "name": "Colmeia Principal",
  "location": "Apiário Norte - Setor A1",
  "ownerId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

**Processo automático**:
1. Valida existência do usuário
2. Verifica se `availableHives > 0`
3. Cria nova colmeia com status INACTIVE
4. Gera UUID e chave de API únicos
5. Decrementa `availableHives` do usuário
6. Retorna dados da colmeia criada

### Fase 4: Gerenciamento Técnico

```
┌─────────────┐                         ┌───────────────┐
│             │  1. Lista todas hives   │               │
│  Técnico    ├────────────────────────>│   Sistema     │
│ TECHNICIAN  │  2. Atualiza status     │               │
│             │  3. Atualiza API key    │               │
│             │  4. Deleta hives        │               │
└─────────────┘                         └───────────────┘
```

**Operações disponíveis**:
- Listar todas as colmeias: `GET /api/technician/hives`
- Ativar colmeia: `PATCH /api/technician/hives/{id}/status` → `ACTIVE`
- Atualizar chave: `PATCH /api/technician/hives/{id}/api-key`
- Remover colmeia: `DELETE /api/technician/hives/{id}`

### Fase 5: Visualização pelo Usuário

```
┌─────────────┐     1. Lista minhas     ┌───────────────┐
│             ├────────────────────────>│               │
│   Usuário   │     hives               │   Sistema     │
│   COMMON    │     2. Vê detalhes      │               │
│             │<────────────────────────┤               │
└─────────────┘     (sem API key)       └───────────────┘
```

**Endpoints do usuário**:
- Listar minhas colmeias: `GET /api/my/hives`
- Ver detalhes específicos: `GET /api/my/hives/{id}` (implementar)

**Diferença importante**: Usuários comuns NÃO veem a chave de API por questões de segurança.

## 6. Implementação das APIs

### Criação de Colmeia

```java
@PostMapping("/technician/hives")
@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
public ResponseEntity<HiveResponse> createHive(@Valid @RequestBody CreateHiveRequest request) {
    Hive hive = hiveUseCase.createHive(request);
    
    HiveResponse response = HiveResponse.builder()
        .id(hive.getId())
        .name(hive.getName())
        .location(hive.getLocation())
        .apiKey(hive.getApiKey())
        .hiveStatus(hive.getHiveStatus())
        .ownerId(hive.getOwner().getId())
        .build();
    
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### Lógica de Negócio na Criação

```java
@Override
@Transactional
public Hive createHive(CreateHiveRequest request) {
    // 1. Validar usuário existe
    User owner = userRepositoryPort.findById(request.ownerId())
            .orElseThrow(() -> new NotFoundException("Usuário dono da colmeia não encontrado"));

    // 2. Validar colmeias disponíveis
    if (owner.getAvailableHives() <= 0) {
        throw new BadRequestException("Usuário não possui colmeias disponíveis.");
    }

    // 3. Criar colmeia
    Hive hive = Hive.builder()
            .name(request.name())
            .location(request.location())
            .apiKey(UUID.randomUUID().toString()) // Chave única
            .hiveStatus(Hive.HiveStatus.INACTIVE) // Sempre inativa inicialmente
            .owner(owner)
            .build();

    // 4. Salvar colmeia
    Hive savedHive = hiveRepositoryPort.save(hive);

    // 5. Decrementar colmeias disponíveis
    owner.setAvailableHives(owner.getAvailableHives() - 1);
    userRepositoryPort.save(owner);

    return savedHive;
}
```

### Listagem com Controle de Acesso

```java
@GetMapping("/my/hives")
public ResponseEntity<Page<GetMyHivesResponse>> getMyHives(Pageable pageable) {
    // Obtém ID do usuário autenticado
    UUID userId = authenticationUtil.getAuthenticatedUserId();
    
    // Busca apenas colmeias do usuário
    Page<Hive> hives = hiveUseCase.listHivesByOwner(userId, pageable);
    
    // Converte para DTO sem chave de API
    Page<GetMyHivesResponse> response = hives.map(hive -> 
        GetMyHivesResponse.builder()
            .id(hive.getId())
            .name(hive.getName())
            .location(hive.getLocation())
            .hiveStatus(hive.getHiveStatus())
            .ownerId(hive.getOwner().getId())
            // Nota: apiKey NÃO é incluída
            .build()
    );
    
    return ResponseEntity.ok(response);
}
```

### Atualização de Status

```java
@PatchMapping("/technician/hives/{hiveId}/status")
@PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
public ResponseEntity<Void> updateHiveStatus(
        @PathVariable UUID hiveId,
        @Valid @RequestBody UpdateHiveStatusRequest request) {
    
    hiveUseCase.updateHiveStatus(hiveId, request.hiveStatus());
    return ResponseEntity.noContent().build();
}
```

### Deleção com Restauração de Disponibilidade

```java
@Override
@Transactional
public void deleteHive(UUID hiveId) {
    // 1. Buscar colmeia
    Hive hive = hiveRepositoryPort.findById(hiveId)
            .orElseThrow(() -> new NotFoundException("Hive não encontrada"));

    // 2. Recuperar proprietário
    User owner = hive.getOwner();
    
    // 3. Restaurar colmeia disponível
    owner.setAvailableHives(owner.getAvailableHives() + 1);
    userRepositoryPort.save(owner);

    // 4. Deletar colmeia
    hiveRepositoryPort.deleteById(hive.getId());
}
```

## 7. Gerenciamento Manual via PostgreSQL (APENAS PARA TESTES E DESENVOLVIMENTO)

> ⚠️ **IMPORTANTE**: As operações descritas nesta seção são **exclusivamente para fins de teste e desenvolvimento**. Em produção, a criação de usuários técnicos e a compra de colmeias serão realizadas através de interfaces automatizadas que ainda estão em desenvolvimento.
>
> Esta seção serve como **suporte temporário** para:
> - Equipe de QA realizar testes funcionais
> - Equupe do EmbarcaTech ou departamento de IOT configurar os dispositivos e a aplicação durante desenvolvimento
> - Desenvolvedores criarem cenários de teste
> - Validação de funcionalidades antes da implementação completa do sistema de compras
> - É ideal que para testes se utilize das ferramentas disponibilizadas como o docker-compose

### Criando um Usuário Técnico Manualmente (SOMENTE PARA TESTES)

> 🔧 **Procedimento Temporário**: Em produção, usuários técnicos serão criados através de um painel administrativo ou processo de contratação automatizado.

```sql
-- 1. Inserir usuário técnico (APENAS AMBIENTE DE DESENVOLVIMENTO)
INSERT INTO users (
    email, 
    password, 
    name, 
    email_verified, 
    role, 
    enabled, 
    auth_provider,
    available_hives
) VALUES (
    'tecnico@techmel.com',
    '$2a$10$example.hashed.password.here', -- Senha hasheada com BCrypt
    'João Técnico Silva',
    true,
    'TECHNICIAN',
    true,
    'LOCAL',
    0 -- Técnicos não precisam de colmeias disponíveis
);

-- 2. Verificar criação
SELECT id, email, name, role, enabled FROM users WHERE email = 'tecnico@techmel.com';
```

### Criando um Usuário Comum com Colmeias Disponíveis (SIMULAÇÃO DE COMPRA)

> 💳 **Simulação Temporária**: Em produção, a compra de colmeias será realizada através de um sistema de e-commerce integrado com gateway de pagamento.
> O usuário pode ser inserido via autenticação (FICA A CRITÉRIO, leia a documentação do [local-auth.md](./LOCAL-AUTH.md))

```sql
-- 1. Inserir usuário comum (APENAS PARA TESTES)
INSERT INTO users (
    email, 
    password, 
    name, 
    email_verified, 
    role, 
    enabled, 
    auth_provider,
    available_hives
) VALUES (
    'cliente@exemplo.com',
    '$2a$10$example.hashed.password.here',
    'Maria Cliente Santos',
    true,
    'COMMON',
    true,
    'LOCAL',
    5 -- SIMULAÇÃO: Cliente com 5 colmeias disponíveis
);
```

### Aumentando Colmeias Disponíveis para um Usuário (SIMULAÇÃO DE COMPRA)

> 🛒 **Artifício Temporário**: Este processo simula uma compra de colmeias. Em produção, isso será automatizado através do sistema de vendas.

```sql
-- Opção 1: Incrementar colmeias disponíveis (SIMULAÇÃO DE COMPRA ADICIONAL)
UPDATE users 
SET available_hives = available_hives + 3,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'cliente@exemplo.com';

-- Opção 2: Definir número específico (PARA TESTES ESPECÍFICOS)
UPDATE users 
SET available_hives = 10,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';

-- Verificar atualização
SELECT name, email, available_hives 
FROM users 
WHERE email = 'cliente@exemplo.com';
```

### Limpeza e Reset de Dados de Teste

> 🧹 **Apenas Ambiente de Desenvolvimento**: Scripts para limpeza rápida durante ciclos de teste.

```sql
-- ⚠️ CUIDADO: Apenas para ambiente de desenvolvimento!
-- NUNCA EXECUTE EM PRODUÇÃO!

-- Deletar todas as colmeias de teste
DELETE FROM hives;

-- Resetar colmeias disponíveis para todos os usuários
UPDATE users SET available_hives = 0 WHERE role = 'COMMON';

-- Deletar usuários de teste (manter admin/técnicos importantes)
DELETE FROM users WHERE email LIKE '%@exemplo.com' OR email LIKE '%@teste.com';
```

## 8. Boas Práticas de Segurança

### Controle de Acesso

1. **Autenticação Obrigatória**: Todos os endpoints requerem token JWT válido
2. **Autorização por Role**: Diferentes níveis de acesso baseados no papel do usuário
3. **Validação de Propriedade**: Usuários só acessam suas próprias colmeias
4. **Sanitização de Dados**: Validação rigorosa de todos os inputs

### Proteção de Dados Sensíveis

1. **Chaves de API**:
   - Geradas com UUID seguro
   - Expostas apenas para técnicos
   - Renovação controlada

2. **Informações Pessoais**:
   - Dados do proprietário limitados ao necessário
   - Logs não devem expor dados sensíveis

### Auditoria e Logs

```java
// Exemplo de log seguro
log.info("Hive criada para o usuário: {}", owner.getId()); // ID, não email
log.warn("Tentativa de criação de hive em usuário que não possui hives disponíveis");
```

### Validações de Entrada

```java
@Schema(description = "Nome da colmeia", example = "Colmeia Principal")
@NotBlank(message = "O nome da colmeia é obrigatório.")
@Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
String name;
```

## 9. Troubleshooting

### Problemas Comuns

#### 1. "Usuário não possui colmeias disponíveis"

**Sintoma**: Erro 400 ao tentar criar colmeia
**Causa**: Campo `available_hives` = 0 ou negativo
**Solução**:
```sql
UPDATE users SET available_hives = 5 WHERE email = 'usuario@exemplo.com';
```

#### 2. "Usuário dono da colmeia não encontrado"

**Sintoma**: Erro 404 ao criar colmeia
**Causa**: UUID do `ownerId` inválido ou usuário inexistente
**Solução**: Verificar se o usuário existe:
```sql
SELECT id, name, email FROM users WHERE id = 'uuid-aqui';
```

#### 3. "Acesso negado" para técnicos

**Sintoma**: Erro 403 mesmo com usuário técnico
**Causa**: Role incorreta no token JWT
**Solução**: Verificar role no banco:
```sql
SELECT email, role FROM users WHERE email = 'tecnico@email.com';
```

#### 4. Colmeias não aparecem para o usuário

**Sintoma**: Lista vazia mesmo com colmeias criadas
**Causa**: Problema na consulta por owner_id
**Solução**: Verificar relacionamento:
```sql
SELECT h.*, u.name FROM hives h 
JOIN users u ON h.owner_id = u.id 
WHERE u.email = 'usuario@email.com';
```

### Logs para Depuração

Ativar logs detalhados no `application-dev.properties`:

```properties
logging.level.com.tech_mel.tech_mel.application.service.HiveService=DEBUG
logging.level.com.tech_mel.tech_mel.infrastructure.api.controller.HiveController=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Consultas de Diagnóstico

```sql
-- Verificar estado geral do sistema
SELECT 
    'Total de usuários' as metric,
    COUNT(*) as value
FROM users
UNION ALL
SELECT 
    'Usuários com colmeias disponíveis',
    COUNT(*)
FROM users WHERE available_hives > 0
UNION ALL
SELECT 
    'Total de colmeias',
    COUNT(*)
FROM hives
UNION ALL
SELECT 
    'Colmeias ativas',
    COUNT(*)
FROM hives WHERE hive_status = 'ACTIVE';
```

## 10. Referências

### Documentação Relacionada
- [Documentação OAuth2](./OAUTH2-AUTH.md)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

### Especificações Técnicas
- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)
- [REST API Best Practices](https://restfulapi.net/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Ferramentas de Desenvolvimento
- [Swagger/OpenAPI](https://swagger.io/)
- [Postman](https://www.postman.com/)
- [pgAdmin](https://www.pgadmin.org/)

---

Esta documentação abrange todos os aspectos do módulo de Hives no sistema TechMel. Para suporte técnico adicional, entre em contato com a equipe de desenvolvimento ou consulte os logs do sistema para diagnósticos específicos.

**⚠️ Nota Importante**: As funcionalidades de compra de colmeias e criação automática de usuários técnicos ainda estão em desenvolvimento. Os scripts SQL fornecidos nesta documentação são **exclusivamente para configuração manual durante a fase de testes e desenvolvimento**.

**🎯 Uso Recomendado dos Scripts Manuais:**
- **Equipe de QA**: Para criar cenários de teste específicos
- **Departamento de IoT**: Para configurar dispositivos durante desenvolvimento
- **Desenvolvedores**: Para validar funcionalidades em ambiente local
- **Testes de Integração**: Para simular diferentes estados do sistema

**🚀 Roadmap de Produção:**
- Sistema de compras automatizado com gateway de pagamento
- Dashboard administrativo para gestão de usuários técnicos
- Integração com sistemas de CRM e financeiro
- Notificações automáticas por email/SMS
- Auditoria completa de transações
