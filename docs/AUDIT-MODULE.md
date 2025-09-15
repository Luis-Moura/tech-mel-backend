# Documentação Técnica: Módulo de Auditoria (Audit) no TechMel

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura da Solução](#2-arquitetura-da-solução)
3. [Entidades e Relacionamentos](#3-entidades-e-relacionamentos)
4. [Casos de Uso (Use Cases)](#4-casos-de-uso-use-cases)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [Integração entre Módulos](#6-integração-entre-módulos)
7. [Implementação das APIs](#7-implementação-das-apis)
8. [Retenção e Limpeza de Dados](#8-retenção-e-limpeza-de-dados)
9. [Análise e Relatórios](#9-análise-e-relatórios)
10. [Exemplos de Interação](#10-exemplos-de-interação)

## 1. Visão Geral

O módulo de Auditoria é o **sistema de rastreabilidade e compliance** do TechMel, responsável por registrar, armazenar e disponibilizar logs detalhados de todas as ações críticas realizadas no sistema. Este módulo garante transparência, segurança e conformidade regulatória através do monitoramento contínuo das operações.

### Características Principais

- **Logging Automático**: Registro automático de todas as ações administrativas
- **Rastreabilidade Completa**: Who, What, When, Where para cada operação
- **Processamento Assíncrono**: Não impacta a performance das operações principais
- **Consultas Avançadas**: Filtros múltiplos e busca por critérios específicos
- **Retenção Configurável**: Políticas de limpeza automática de dados antigos
- **Estatísticas Operacionais**: Dashboards e métricas de auditoria
- **Exportação de Dados**: Relatórios para compliance e análise forense

### Tipos de Eventos Auditados

```
┌─────────────────────┐
│ AÇÕES DE USUÁRIO    │
│ • LOGIN/LOGOUT      │
│ • REGISTER          │
│ • PASSWORD_CHANGE   │
│ • PASSWORD_RESET    │
│ • ACCOUNT_LOCKED    │
│ • ACCOUNT_UNLOCKED  │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ AÇÕES ADMINISTRATIVAS│
│ • CREATE            │
│ • UPDATE            │
│ • DELETE            │
│ • ACTIVATE          │
│ • DEACTIVATE        │
└─────────────────────┘
           │
           ▼
┌─────────────────────┐
│ AÇÕES DO SISTEMA    │
│ • SCHEDULED_TASK    │
│ • DATA_CLEANUP      │
│ • SYSTEM_ERROR      │
│ • SECURITY_ALERT    │
└─────────────────────┘
```

## 2. Arquitetura da Solução

### Diagrama de Componentes

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Todos os      │    │   Audit         │    │   PostgreSQL    │
│   Módulos       │    │   Service       │    │   Database      │
│                 │    │                 │    │                 │
│ • AdminService  │───►│ • Log Actions   │───►│ • audit_logs    │
│ • AuthService   │    │ • Async Proc    │    │ • Indexação     │
│ • HiveService   │    │ • Filtering     │    │ • Partitioning  │
│ • UserService   │    │ • Statistics    │    │ • Archiving     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Audit         │
                       │   Dashboard     │
                       │                 │
                       │ • Search Logs   │
                       │ • Analytics     │
                       │ • Export Data   │
                       │ • Compliance    │
                       └─────────────────┘
```

### Componentes Principais

1. **AuditController**: Endpoints REST para consulta de logs
2. **AuditService**: Lógica de negócio e processamento assíncrono
3. **AuditRepositoryPort**: Interface para persistência no PostgreSQL
4. **AuditLog**: Entidade de domínio para logs de auditoria
5. **AuditCleanupScheduler**: Job automatizado para limpeza de dados antigos
6. **FilteringEngine**: Sistema avançado de busca e filtros

## 3. Entidades e Relacionamentos

### Entidade AuditLog

```java
public class AuditLog {
    private UUID id;                    // Identificador único
    private UUID userId;                // ID do usuário que executou a ação
    private String userName;            // Nome do usuário (desnormalizado)
    private String userEmail;           // Email do usuário (desnormalizado)
    private AuditAction action;         // Tipo de ação executada
    private EntityType entityType;      // Tipo de entidade afetada
    private String entityId;            // ID da entidade afetada
    private String details;             // Descrição detalhada da ação
    private String ipAddress;           // IP de origem da requisição
    private String userAgent;           // Browser/client info
    private LocalDateTime timestamp;    // Timestamp da ação
    private String oldValues;           // Valores anteriores (JSON)
    private String newValues;           // Valores novos (JSON)
    private boolean success;            // Sucesso ou falha da operação
    private String errorMessage;        // Mensagem de erro (se aplicável)
}
```

### Enums de Auditoria

#### AuditAction

```java
public enum AuditAction {
    // Ações de Usuário
    LOGIN, LOGOUT, REGISTER, EMAIL_VERIFICATION,
    PASSWORD_CHANGE, PASSWORD_RESET, FORGOT_PASSWORD,

    // Ações Administrativas
    CREATE, UPDATE, DELETE, ACTIVATE, DEACTIVATE,
    ACCOUNT_LOCKED, ACCOUNT_UNLOCKED,

    // Ações do Sistema
    SCHEDULED_TASK, DATA_CLEANUP, SYSTEM_ERROR,
    SECURITY_ALERT, ACCESS_DENIED
}
```

#### EntityType

```java
public enum EntityType {
    USER,           // Usuários do sistema
    HIVE,           // Colmeias
    MEASUREMENT,    // Medições dos sensores
    ALERT,          // Alertas gerados
    THRESHOLD,      // Limites configurados
    AUDIT,          // Logs de auditoria
    SYSTEM          // Ações do sistema
}
```

### Estrutura de Dados no PostgreSQL

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    user_name VARCHAR(255),
    user_email VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255),
    details TEXT,
    ip_address INET,
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_values JSONB,
    new_values JSONB,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,

    -- Índices para performance
    INDEX idx_audit_timestamp (timestamp DESC),
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity_type (entity_type),
    INDEX idx_audit_success (success),
    INDEX idx_audit_date_action (DATE(timestamp), action)
) PARTITION BY RANGE (timestamp);

-- Particionamento por mês para melhor performance
CREATE TABLE audit_logs_y2025m01 PARTITION OF audit_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

## 4. Casos de Uso (Use Cases)

### Interface AuditUseCase

```java
public interface AuditUseCase {
    // Logging de Ações
    void logAction(UUID userId, AuditAction action, EntityType entityType,
                   String entityId, String details);
    CompletableFuture<AuditLog> logAction(UUID userId, AuditAction action,
                                         EntityType entityType, String entityId,
                                         String details, String oldValues, String newValues,
                                         String ipAddress, String userAgent);

    // Consultas de Logs
    Page<AuditLog> getAllAuditLogs(Pageable pageable);
    Page<AuditLog> getAuditLogsByUser(UUID userId, Pageable pageable);
    Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable);
    Page<AuditLog> getAuditLogsByEntityType(EntityType entityType, Pageable pageable);
    Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                          Pageable pageable);
    Page<AuditLog> getAuditLogsWithFilters(UUID userId, AuditAction action,
                                          EntityType entityType, LocalDateTime startDate,
                                          LocalDateTime endDate, Boolean success,
                                          Pageable pageable);

    // Análises e Estatísticas
    List<AuditLog> getRecentUserActivity(UUID userId, int limit);
    Map<String, Long> getAuditStatisticsByAction();
    Map<String, Long> getAuditStatisticsByEntityType();
    long getTotalAuditRecords();
    long getFailedActionsCount();

    // Manutenção
    void cleanupOldAuditRecords(int retentionDays);
    List<AuditLog> exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate);
}
```

### Casos de Uso Detalhados

#### UC01: Registro Automático de Ação

- **Ator**: Sistema (qualquer módulo)
- **Pré-condição**: Ação executada por usuário autenticado
- **Fluxo Principal**:
  1. Módulo executa ação (ex: AdminService.createTechnician)
  2. Módulo invoca auditUseCase.logAction() após operação
  3. AuditService processa log de forma assíncrona
  4. Sistema persiste log no PostgreSQL
  5. Log fica disponível para consulta imediatamente

#### UC02: Consulta de Logs com Filtros

- **Ator**: Administrador
- **Pré-condição**: Usuário com role ADMIN
- **Fluxo Principal**:
  1. Admin acessa dashboard de auditoria
  2. Admin aplica filtros (usuário, ação, período, etc.)
  3. Sistema executa query otimizada com filtros
  4. Sistema retorna logs paginados
  5. Admin visualiza detalhes e pode exportar

#### UC03: Análise de Atividade Suspeita

- **Ator**: Administrador
- **Pré-condição**: Suspeita de atividade irregular
- **Fluxo Principal**:
  1. Admin pesquisa logs por usuário específico
  2. Admin analisa padrão de ações e timestamps
  3. Admin identifica ações anômalas ou fora do padrão
  4. Admin pode bloquear usuário e investigar mais

#### UC04: Compliance e Relatórios

- **Ator**: Administrador/Auditor Externo
- **Pré-condição**: Necessidade de relatório de compliance
- **Fluxo Principal**:
  1. Usuário define período para relatório
  2. Sistema exporta logs do período especificado
  3. Sistema gera relatório em formato estruturado
  4. Relatório pode ser usado para auditoria externa

#### UC05: Limpeza Automática de Dados

- **Ator**: Sistema (Scheduler)
- **Pré-condição**: Job configurado para execução periódica
- **Fluxo Principal**:
  1. Scheduler executa job de limpeza
  2. Sistema identifica logs mais antigos que política de retenção
  3. Sistema arquiva ou remove logs antigos
  4. Sistema libera espaço em disco e otimiza performance

## 5. Fluxo de Dados

### Fluxo de Logging Assíncrono

```
1. Módulo Fonte (AdminService)
   │ adminService.createTechnician(...)
   │ // operação principal executada
   │ auditUseCase.logAction(userId, CREATE, USER, userID, details)
   ▼
2. AuditService (@Async)
   │ @Async CompletableFuture<AuditLog> logAction(...)
   │ ├─ userRepositoryPort.findById(userId) // obter dados do user
   │ ├─ build AuditLog object
   │ ├─ auditRepositoryPort.save(auditLog)
   │ └─ return CompletableFuture.completedFuture(savedLog)
   ▼
3. PostgreSQL Persistence
   │ INSERT INTO audit_logs (
   │   user_id, user_name, action, entity_type, entity_id,
   │   details, timestamp, success, ...
   │ ) VALUES (...)
   ▼
4. Background Processing
   │ Log disponível imediatamente para consulta
   │ Índices atualizados automaticamente
   │ Particionamento aplicado se configurado
```

### Fluxo de Consulta com Filtros

```
1. Dashboard Request
   │ GET /api/admin/audit/logs?userId=X&action=CREATE&startDate=Y&endDate=Z
   ▼
2. AuditController
   │ @PreAuthorize("hasRole('ADMIN')")
   │ AuditLogFilterRequest filters
   ▼
3. AuditService.getAuditLogsWithFilters()
   │ auditRepositoryPort.findByFilters(
   │   userId, action, entityType, startDate, endDate, success, pageable
   │ )
   ▼
4. Dynamic Query Building
   │ SELECT * FROM audit_logs
   │ WHERE user_id = ?
   │   AND action = ?
   │   AND timestamp BETWEEN ? AND ?
   │ ORDER BY timestamp DESC
   │ LIMIT ? OFFSET ?
   ▼
5. Response Formatting
   │ Page<AuditLog> → Page<AuditLogResponse>
   │ Mapeamento de entidades para DTOs
   ▼
6. JSON Response
   │ Logs paginados com metadados de paginação
```

### Fluxo de Análise Estatística

```
1. Statistics Request
   │ GET /api/admin/audit/statistics
   ▼
2. Parallel Data Aggregation
   │ ├─ getAuditStatisticsByAction()
   │ ├─ getAuditStatisticsByEntityType()
   │ ├─ getTotalAuditRecords()
   │ └─ getFailedActionsCount()
   ▼
3. Optimized Queries
   │ SELECT action, COUNT(*) FROM audit_logs GROUP BY action;
   │ SELECT entity_type, COUNT(*) FROM audit_logs GROUP BY entity_type;
   │ SELECT COUNT(*) FROM audit_logs;
   │ SELECT COUNT(*) FROM audit_logs WHERE success = false;
   ▼
4. Metrics Compilation
   │ Map<String, Object> statistics = {
   │   "totalRecords": total,
   │   "failedActions": failed,
   │   "successRate": (total - failed) / total * 100,
   │   "actionStatistics": actionStats,
   │   "entityStatistics": entityStats
   │ }
   ▼
5. Dashboard Metrics
   │ Dados formatados para visualização em gráficos
```

## 6. Integração entre Módulos

### Todos os Módulos → Audit (Logging Universal)

```java
// Padrão de integração para logging
@Service
public class AdminService {
    private final AuditUseCase auditUseCase;

    public User createTechnician(...) {
        User savedUser = userRepositoryPort.save(user);

        // Log da ação administrativa
        auditUseCase.logAction(
            getCurrentAdminId(),
            AuditAction.CREATE,
            EntityType.USER,
            savedUser.getId().toString(),
            "Técnico criado: " + savedUser.getEmail()
        );

        return savedUser;
    }
}
```

**Módulos Integrados**:

- **AdminService**: Todas as operações administrativas
- **AuthService**: Login, logout, mudanças de senha
- **HiveService**: Criação e gestão de colmeias
- **UserService**: Operações de usuário
- **SystemServices**: Tarefas agendadas e operações de sistema

### Audit → User (Desnormalização para Performance)

```java
// Dados do usuário são desnormalizados no log para evitar JOINs
User user = userRepositoryPort.findById(userId).orElse(null);

AuditLog auditLog = AuditLog.builder()
    .userId(userId)
    .userName(user != null ? user.getName() : "Unknown")
    .userEmail(user != null ? user.getEmail() : "Unknown")
    // ... outros campos
    .build();
```

**Vantagens da Desnormalização**:

- **Performance**: Evita JOINs custosos em consultas
- **Integridade Histórica**: Preserva dados mesmo se usuário for deletado
- **Simplicidade**: Queries diretas na tabela de audit
- **Resiliência**: Funciona mesmo com dados inconsistentes

### Audit → Security (Detecção de Anomalias)

```java
// Futuro: Integração com sistema de segurança
@EventListener
public void onSuspiciousActivity(SuspiciousActivityEvent event) {
    auditUseCase.logAction(
        event.getUserId(),
        AuditAction.SECURITY_ALERT,
        EntityType.SYSTEM,
        "SECURITY_SYSTEM",
        "Atividade suspeita detectada: " + event.getDescription(),
        null, null,
        event.getIpAddress(),
        event.getUserAgent()
    );
}
```

## 7. Implementação das APIs

### Endpoints Principais

#### GET /api/admin/audit/logs

**Listar logs com filtros avançados**

```bash
curl -X GET \
  'http://localhost:8080/api/admin/audit/logs?page=0&size=20&sortBy=timestamp&sortDirection=desc&userId=user-uuid&action=CREATE&startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59' \
  -H 'Authorization: Bearer {admin-token}'
```

**Resposta**:

```json
{
	"content": [
		{
			"id": "audit-log-uuid-1",
			"userId": "admin-uuid",
			"userName": "Admin Principal",
			"userEmail": "admin@techmel.com",
			"action": "CREATE",
			"entityType": "USER",
			"entityId": "new-user-uuid",
			"details": "Técnico criado: tecnico@techmel.com",
			"ipAddress": "192.168.1.100",
			"userAgent": "Mozilla/5.0...",
			"timestamp": "2025-01-15T14:30:00Z",
			"oldValues": null,
			"newValues": "{\"email\":\"tecnico@techmel.com\",\"role\":\"TECHNICIAN\"}",
			"success": true,
			"errorMessage": null
		}
	],
	"totalElements": 1250,
	"totalPages": 63,
	"size": 20,
	"number": 0
}
```

#### GET /api/admin/audit/logs/user/{userId}

**Logs específicos de um usuário**

```bash
curl -X GET \
  'http://localhost:8080/api/admin/audit/logs/user/f47ac10b-58cc-4372-a567-0e02b2c3d479?page=0&size=10' \
  -H 'Authorization: Bearer {admin-token}'
```

#### GET /api/admin/audit/statistics

**Estatísticas de auditoria**

```bash
curl -X GET \
  'http://localhost:8080/api/admin/audit/statistics' \
  -H 'Authorization: Bearer {admin-token}'
```

**Resposta**:

```json
{
	"totalRecords": 15420,
	"failedActions": 23,
	"successRate": 99.85,
	"actionStatistics": {
		"LOGIN": 8230,
		"CREATE": 1250,
		"UPDATE": 890,
		"DELETE": 45,
		"PASSWORD_RESET": 123,
		"ACCOUNT_LOCKED": 8
	},
	"entityStatistics": {
		"USER": 5430,
		"HIVE": 3200,
		"MEASUREMENT": 4800,
		"ALERT": 1990,
		"THRESHOLD": 890
	},
	"generatedAt": "2025-01-15T16:45:00Z"
}
```

#### GET /api/admin/audit/logs/export

**Exportar logs para relatório**

```bash
curl -X GET \
  'http://localhost:8080/api/admin/audit/logs/export?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59' \
  -H 'Authorization: Bearer {admin-token}' \
  --output audit-report-january-2025.json
```

#### POST /api/admin/audit/maintenance/cleanup

**Limpeza de logs antigos (admin primário apenas)**

```bash
curl -X POST \
  'http://localhost:8080/api/admin/audit/maintenance/cleanup?retentionDays=365' \
  -H 'Authorization: Bearer {primary-admin-token}'
```

### Códigos de Resposta

- **200 OK**: Consulta realizada com sucesso
- **400 Bad Request**: Filtros inválidos ou parâmetros incorretos
- **401 Unauthorized**: Token inválido ou expirado
- **403 Forbidden**: Privilégios insuficientes para operação
- **404 Not Found**: Logs não encontrados para os critérios especificados

## 8. Retenção e Limpeza de Dados

### Política de Retenção

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditCleanupScheduler {

    private final AuditUseCase auditUseCase;

    @Value("${app.audit.retention-days:365}")
    private int defaultRetentionDays;

    // Executa todo domingo às 02:00
    @Scheduled(cron = "0 0 2 * * SUN")
    public void cleanupOldAuditLogs() {
        log.info("Iniciando limpeza automática de logs de auditoria");
        auditUseCase.cleanupOldAuditRecords(defaultRetentionDays);
        log.info("Limpeza de logs de auditoria concluída");
    }
}
```

### Configuração de Retenção

```properties
# application.properties
app.audit.retention-days=365        # Padrão: 1 ano
app.audit.cleanup.enabled=true      # Habilitar limpeza automática
app.audit.cleanup.batch-size=1000   # Registros por batch
app.audit.archive.enabled=false     # Arquivamento antes da exclusão
```

### Estratégias de Limpeza

#### Limpeza Simples (Deleção)

```java
public void cleanupOldAuditRecords(int retentionDays) {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

    log.info("Removendo logs de auditoria anteriores a {}", cutoffDate);
    long deletedRecords = auditRepositoryPort.deleteOldRecords(cutoffDate);
    log.info("Limpeza concluída. {} registros removidos", deletedRecords);
}
```

#### Limpeza com Arquivamento

```java
public void archiveAndCleanupOldAuditRecords(int retentionDays) {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

    // 1. Exportar para arquivo
    List<AuditLog> oldLogs = auditRepositoryPort.findOldRecords(cutoffDate);
    String archiveFile = createArchiveFile(oldLogs, cutoffDate);

    // 2. Deletar do banco
    long deletedRecords = auditRepositoryPort.deleteOldRecords(cutoffDate);

    log.info("Logs arquivados em {} e {} registros removidos",
             archiveFile, deletedRecords);
}
```

### Otimizações de Performance

#### Particionamento por Data

```sql
-- Criar partições mensais automaticamente
CREATE OR REPLACE FUNCTION create_audit_partition(start_date DATE)
RETURNS VOID AS $$
DECLARE
    table_name TEXT;
    end_date DATE;
BEGIN
    table_name := 'audit_logs_y' || EXTRACT(YEAR FROM start_date) || 'm' ||
                  LPAD(EXTRACT(MONTH FROM start_date)::TEXT, 2, '0');
    end_date := start_date + INTERVAL '1 month';

    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs
                    FOR VALUES FROM (%L) TO (%L)',
                   table_name, start_date, end_date);
END;
$$ LANGUAGE plpgsql;

-- Job para criar partições futuras
SELECT create_audit_partition(DATE_TRUNC('month', CURRENT_DATE + INTERVAL '1 month'));
```

#### Índices Otimizados

```sql
-- Índices compostos para consultas comuns
CREATE INDEX CONCURRENTLY idx_audit_user_timestamp
    ON audit_logs (user_id, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_audit_action_timestamp
    ON audit_logs (action, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_audit_entity_timestamp
    ON audit_logs (entity_type, entity_id, timestamp DESC);

-- Índice para cleanup
CREATE INDEX CONCURRENTLY idx_audit_timestamp_cleanup
    ON audit_logs (timestamp) WHERE timestamp < NOW() - INTERVAL '30 days';
```

## 9. Análise e Relatórios

### Dashboard de Auditoria

```typescript
interface AuditDashboardProps {}

const AuditDashboard: React.FC<AuditDashboardProps> = () => {
	const [statistics, setStatistics] = useState<AuditStatistics | null>(null);
	const [recentLogs, setRecentLogs] = useState<AuditLog[]>([]);
	const [filters, setFilters] = useState<AuditFilters>({});

	const fetchStatistics = async () => {
		const response = await api.get("/admin/audit/statistics");
		setStatistics(response.data);
	};

	const fetchRecentLogs = async () => {
		const response = await api.get(
			"/admin/audit/logs?size=10&sortBy=timestamp&sortDirection=desc"
		);
		setRecentLogs(response.data.content);
	};

	return (
		<div className="audit-dashboard">
			<h1>Dashboard de Auditoria</h1>

			{/* Métricas Principais */}
			<div className="metrics-grid">
				<MetricCard
					title="Total de Logs"
					value={statistics?.totalRecords}
					icon="📋"
				/>
				<MetricCard
					title="Taxa de Sucesso"
					value={`${statistics?.successRate.toFixed(2)}%`}
					icon="✅"
					status={statistics?.successRate > 99 ? "good" : "warning"}
				/>
				<MetricCard
					title="Ações Falharam"
					value={statistics?.failedActions}
					icon="❌"
					status={statistics?.failedActions < 50 ? "good" : "warning"}
				/>
			</div>

			{/* Gráficos de Distribuição */}
			<div className="charts-section">
				<div className="chart-container">
					<h3>Distribuição por Tipo de Ação</h3>
					<BarChart data={statistics?.actionStatistics} />
				</div>
				<div className="chart-container">
					<h3>Distribuição por Entidade</h3>
					<PieChart data={statistics?.entityStatistics} />
				</div>
			</div>

			{/* Filtros Avançados */}
			<AuditFiltersPanel
				filters={filters}
				onFiltersChange={setFilters}
				onSearch={() => searchLogs(filters)}
			/>

			{/* Logs Recentes */}
			<div className="recent-logs">
				<h3>Atividade Recente</h3>
				<AuditLogTable logs={recentLogs} />
			</div>

			{/* Ações Rápidas */}
			<div className="quick-actions">
				<button onClick={() => exportReport("daily")}>Relatório Diário</button>
				<button onClick={() => exportReport("weekly")}>
					Relatório Semanal
				</button>
				<button onClick={() => openUserActivity()}>Analisar Usuário</button>
			</div>
		</div>
	);
};
```

### Relatórios de Compliance

```java
@Service
public class AuditReportService {

    public ComplianceReport generateComplianceReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> logs = auditUseCase.exportAuditLogs(startDate, endDate);

        return ComplianceReport.builder()
            .period(Period.between(startDate.toLocalDate(), endDate.toLocalDate()))
            .totalActions(logs.size())
            .successfulActions(logs.stream().mapToLong(log -> log.isSuccess() ? 1 : 0).sum())
            .failedActions(logs.stream().mapToLong(log -> !log.isSuccess() ? 1 : 0).sum())
            .userActivitySummary(generateUserActivitySummary(logs))
            .actionDistribution(generateActionDistribution(logs))
            .securityEvents(filterSecurityEvents(logs))
            .administrativeActions(filterAdministrativeActions(logs))
            .generatedAt(LocalDateTime.now())
            .build();
    }

    public SecurityAnalysisReport analyzeSecurityPatterns(UUID userId, Period period) {
        List<AuditLog> userLogs = auditUseCase.getAuditLogsByUser(userId, Pageable.unpaged()).getContent();

        return SecurityAnalysisReport.builder()
            .userId(userId)
            .analysisParameters(period)
            .loginPatterns(analyzeLoginPatterns(userLogs))
            .suspiciousActivities(detectSuspiciousActivities(userLogs))
            .accessPatterns(analyzeAccessPatterns(userLogs))
            .riskScore(calculateRiskScore(userLogs))
            .recommendations(generateSecurityRecommendations(userLogs))
            .build();
    }
}
```

## 10. Exemplos de Interação

### Cenário 1: Investigação de Atividade Suspeita

#### Passo 1: Detectar Padrão Anômalo

```bash
# Admin nota múltiplas tentativas de reset de senha
GET /api/admin/audit/logs?action=PASSWORD_RESET&startDate=2025-01-15T00:00:00&endDate=2025-01-15T23:59:59

# Resposta mostra 15 tentativas de reset em 1 dia (anômalo)
{
  "content": [
    {
      "action": "PASSWORD_RESET",
      "userEmail": "suspeito@example.com",
      "ipAddress": "203.0.113.45",
      "timestamp": "2025-01-15T14:22:00Z",
      "success": false,
      "errorMessage": "Too many reset attempts"
    },
    // ... mais 14 tentativas
  ]
}
```

#### Passo 2: Analisar Atividade do Usuário

```bash
# Buscar todas as ações do usuário suspeito
GET /api/admin/audit/logs/user/suspeito-uuid?size=50

# Analisar padrão de IPs e horários
# Verificar se há login de IPs diferentes simultaneamente
# Identificar ações fora do padrão normal
```

#### Passo 3: Ação Preventiva

```bash
# Bloquear usuário suspeito
POST /api/admin/users/suspeito-uuid/lock

# Esta ação gerará automaticamente um log:
{
  "action": "ACCOUNT_LOCKED",
  "entityType": "USER",
  "entityId": "suspeito-uuid",
  "details": "Usuário bloqueado: suspeito@example.com",
  "userId": "admin-uuid",
  "userName": "Admin Segurança"
}
```

### Cenário 2: Relatório de Compliance Mensal

#### Geração Automática

```java
@Scheduled(cron = "0 0 1 1 * *") // Todo 1º do mês às 00:00
public void generateMonthlyComplianceReport() {
    LocalDateTime endDate = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).minusDays(1);
    LocalDateTime startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());

    List<AuditLog> monthlyLogs = auditUseCase.exportAuditLogs(startDate, endDate);

    ComplianceReport report = ComplianceReport.builder()
        .period(startDate + " to " + endDate)
        .totalActions(monthlyLogs.size())
        .successRate(calculateSuccessRate(monthlyLogs))
        .criticalActions(filterCriticalActions(monthlyLogs))
        .userActivity(generateUserActivitySummary(monthlyLogs))
        .build();

    // Salvar relatório e enviar para stakeholders
    saveReport(report);
    notifyStakeholders(report);
}
```

### Cenário 3: Análise de Performance de Auditoria

#### Monitoramento de Volume

```typescript
const AuditMetricsComponent: React.FC = () => {
	const [metrics, setMetrics] = useState<AuditMetrics>();

	useEffect(() => {
		const fetchMetrics = async () => {
			const stats = await api.get("/admin/audit/statistics");

			// Calcular métricas de performance
			const avgLogsPerDay = stats.data.totalRecords / 30; // último mês
			const storageUsed = estimateStorageUsage(stats.data.totalRecords);

			setMetrics({
				totalLogs: stats.data.totalRecords,
				avgLogsPerDay: Math.round(avgLogsPerDay),
				storageUsed: formatBytes(storageUsed),
				failureRate: (stats.data.failedActions / stats.data.totalRecords) * 100,
				topActions: Object.entries(stats.data.actionStatistics).sort(
					([, a], [, b]) => b - a
				),
			});
		};

		fetchMetrics();
	}, []);

	return (
		<div className="audit-metrics">
			<h3>Métricas de Auditoria</h3>

			<div className="metrics-grid">
				<div className="metric">
					<span className="label">Logs por Dia (Média)</span>
					<span className="value">{metrics?.avgLogsPerDay}</span>
				</div>
				<div className="metric">
					<span className="label">Armazenamento Usado</span>
					<span className="value">{metrics?.storageUsed}</span>
				</div>
				<div className="metric">
					<span className="label">Taxa de Falha</span>
					<span className="value">{metrics?.failureRate.toFixed(2)}%</span>
				</div>
			</div>

			<div className="top-actions">
				<h4>Ações Mais Frequentes</h4>
				{metrics?.topActions.map(([action, count]) => (
					<div key={action} className="action-bar">
						<span>{action}</span>
						<div className="bar-container">
							<div
								className="bar-fill"
								style={{
									width: `${(count / metrics.topActions[0][1]) * 100}%`,
								}}
							/>
						</div>
						<span>{count}</span>
					</div>
				))}
			</div>
		</div>
	);
};
```

### Cenário 4: Limpeza de Dados Antigos

#### Execução Manual por Admin Primário

```bash
# Admin primário executa limpeza de logs com mais de 2 anos
POST /api/admin/audit/maintenance/cleanup?retentionDays=730
Authorization: Bearer {primary-admin-token}

# Sistema responde:
HTTP 200 OK
# Log gerado automaticamente:
{
  "action": "DATA_CLEANUP",
  "entityType": "AUDIT",
  "details": "Limpeza manual de logs de auditoria - retenção: 730 dias",
  "userId": "primary-admin-uuid",
  "success": true
}
```

#### Monitoramento do Processo

```sql
-- Query para monitorar progresso da limpeza
SELECT
    DATE_TRUNC('month', timestamp) as month,
    COUNT(*) as logs_count,
    MIN(timestamp) as oldest_log,
    MAX(timestamp) as newest_log
FROM audit_logs
GROUP BY DATE_TRUNC('month', timestamp)
ORDER BY month DESC
LIMIT 24; -- últimos 24 meses
```

---

Esta documentação cobre todos os aspectos do módulo de Auditoria no TechMel. O sistema é projetado para fornecer rastreabilidade completa, performance otimizada e ferramentas robustas de análise para garantir a segurança e compliance do sistema.
