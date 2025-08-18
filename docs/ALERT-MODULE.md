# Documentação Técnica: Módulo de Alertas no TechMel

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura da Solução](#2-arquitetura-da-solução)
3. [Entidades e Relacionamentos](#3-entidades-e-relacionamentos)
4. [Casos de Uso (Use Cases)](#4-casos-de-uso-use-cases)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [Integração entre Módulos](#6-integração-entre-módulos)
7. [Implementação das APIs](#7-implementação-das-apis)
8. [Cálculo de Severidade](#8-cálculo-de-severidade)
9. [Gerenciamento via APIs](#9-gerenciamento-via-apis)
10. [Exemplos de Interação](#10-exemplos-de-interação)
11. [Possíveis Extensões Futuras](#11-possíveis-extensões-futuras)

## 1. Visão Geral

O módulo de Alertas do TechMel é responsável por **detectar, classificar e gerenciar alertas** gerados quando as medições dos sensores IoT extrapolam os limites configurados nos thresholds (limiares). Este sistema funciona como o "sistema nervoso" da plataforma, alertando os apicultores sobre condições ambientais críticas que podem afetar a saúde das colmeias.

### Características Principais

- **Detecção Automática**: Alertas são criados automaticamente quando medições excedem os limites
- **Classificação Inteligente**: Sistema de severidade baseado na distância do valor do threshold
- **Gestão de Status**: Controle do ciclo de vida dos alertas (NEW → VIEWED → RESOLVED)
- **Filtragem e Busca**: APIs para consultar alertas por hive, status e outros critérios
- **Integração Temporal**: Alertas são persistidos com timestamp preciso para auditoria

### Tipos de Alertas Suportados

- **TEMPERATURE**: Alertas de temperatura fora dos limites ideais
- **HUMIDITY**: Alertas de umidade relativa inadequada
- **CO2**: Alertas de concentração de CO₂ prejudicial

## 2. Arquitetura da Solução

### Diagrama de Componentes

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Measurement   │    │      Alert      │    │   PostgreSQL    │
│    Service      │    │     Service     │    │    Database     │
│                 │    │                 │    │                 │
│ • Validação     │◄──►│ • Criação       │◄──►│ • alert         │
│ • Persistência  │    │ • Classificação │    │ • threshold     │
│   no Redis      │    │ • Gerenciamento │    │ • hives         │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Threshold     │
                       │    Service      │
                       │                 │
                       │ • Configuração  │
                       │ • Limites       │
                       │                 │
                       └─────────────────┘
```

### Componentes Principais

1. **AlertController**: Endpoints REST para consulta e gerenciamento de alertas
2. **AlertService**: Lógica de negócio para criação, classificação e gestão de alertas
3. **AlertRepositoryPort**: Interface para persistência no PostgreSQL
4. **AlertMapper**: Conversão entre entidades JPA e objetos de domínio
5. **AlertEntity**: Entidade JPA para mapeamento da tabela
6. **DTOs**: Objetos de transferência de dados (Request/Response)

## 3. Entidades e Relacionamentos

### Entidade Alert

```java
public class Alert {
    private UUID id;
    private LocalDateTime timestamp;
    private AlertType type;         // TEMPERATURE, HUMIDITY, CO2
    private AlertSeverity severity; // LOW, MEDIUM, HIGH
    private Double value;           // Valor que gerou o alerta
    private AlertStatus status;     // NEW, VIEWED, RESOLVED
    private Hive hive;             // Relação com a colmeia
}
```

### Relacionamentos

- **Alert ↔ Hive**: Relacionamento Many-to-One (vários alertas para uma colmeia)
- **Alert ↔ Threshold**: Relação conceitual através do HiveId (usado para cálculo de severidade)
- **Cascade Delete**: Quando uma colmeia é deletada, seus alertas também são removidos
- **Índices**: Otimização para consultas por hive_id e status

### Estados dos Alertas

```
┌─────┐     usuário     ┌────────┐     usuário     ┌──────────┐
│ NEW │ ──────────────► │ VIEWED │ ──────────────► │ RESOLVED │
└─────┘     visualiza   └────────┘    soluciona   └──────────┘
    ▲                                                   │
    │                                                   │
    └───────────────────── pode reabrir ────────────────┘
```

## 4. Casos de Uso (Use Cases)

### Interface AlertUseCase

```java
public interface AlertUseCase {
    void saveAlert(Measurement measurement, Hive hive, LocalDateTime timestamp);
    Alert getAlertById(UUID alertId, UUID ownerId);
    Page<Alert> getAlertsByHiveIdAndStatus(UUID hiveId, Alert.AlertStatus status,
                                          UUID ownerId, Pageable pageable);
    void updateAlertStatus(UUID alertId, Alert.AlertStatus status, UUID ownerId);
}
```

### Casos de Uso Detalhados

#### UC01: Criação Automática de Alertas

- **Ator**: Sistema (MeasurementService)
- **Pré-condição**: Medição recebida e threshold configurado
- **Fluxo Principal**:
  1. Sistema recebe nova medição
  2. Compara valores com thresholds da colmeia
  3. Para cada valor fora do limite, cria alerta com severidade calculada
  4. Persiste alertas no PostgreSQL

#### UC02: Consulta de Alertas por Colmeia

- **Ator**: Usuário Common/Technician/Admin
- **Pré-condição**: Usuário autenticado e proprietário da colmeia
- **Fluxo Principal**:
  1. Cliente faz requisição GET com hiveId
  2. Sistema valida propriedade da colmeia
  3. Retorna alertas paginados com filtros opcionais

#### UC03: Atualização de Status de Alerta

- **Ator**: Usuário Common/Technician/Admin
- **Pré-condição**: Alerta existe e usuário é proprietário
- **Fluxo Principal**:
  1. Cliente envia PATCH com novo status
  2. Sistema valida propriedade do alerta
  3. Atualiza status e persiste mudança

#### UC04: Consulta de Alerta Individual

- **Ator**: Usuário autenticado
- **Pré-condição**: Alerta existe e usuário tem acesso
- **Fluxo Principal**:
  1. Cliente requisita alerta por ID
  2. Sistema valida acesso através da colmeia
  3. Retorna dados completos do alerta

## 5. Fluxo de Dados

### Fluxo de Criação de Alertas

```
1. IoT Device
   │ POST /api/measurements/iot
   ▼
2. MeasurementController
   │ validateApiKey(apiKey)
   ▼
3. MeasurementService
   │ registerMeasurement()
   │ └─ saveToRedis()
   │ └─ alertUseCase.saveAlert()
   ▼
4. AlertService
   │ findThresholdByHiveId()
   │ └─ checkTemperature()
   │ └─ checkHumidity()
   │ └─ checkCO2()
   │ └─ calculateSeverity()
   │ └─ buildAlert()
   │ └─ saveToPostgreSQL()
   ▼
5. Alerta Persistido
```

### Fluxo de Consulta de Alertas

```
1. Frontend/Client
   │ GET /api/alerts/hive/{hiveId}?status=NEW
   ▼
2. AlertController
   │ getCurrentUserId()
   │ validateAccess()
   ▼
3. AlertService
   │ validateHiveOwnership()
   │ findAlertsByHiveAndStatus()
   ▼
4. AlertRepositoryAdapter
   │ findAllByHiveIdAndStatus()
   ▼
5. PostgreSQL
   │ SELECT com JOIN para verificar ownership
   ▼
6. Response Paginado
```

## 6. Integração entre Módulos

### Alert ↔ Measurement

```java
// No MeasurementService após salvar no Redis
alertUseCase.saveAlert(measurement, hive, request.measuredAt());
```

A integração é **síncrona** e **automática**:

- Toda nova medição dispara verificação de alertas
- Alertas são criados na mesma transação da medição
- Falha na criação de alerta não impede registro da medição

### Alert ↔ Threshold

```java
// No AlertService para buscar limites
Threshold threshold = thresholdRepositoryPort.findByHiveId(hive.getId())
    .orElseThrow(() -> new NotFoundException("Threshold not configured"));
```

Dependência **obrigatória**:

- Alertas só podem ser criados se threshold estiver configurado
- Severidade é calculada com base nos limites do threshold
- Mudanças no threshold afetam apenas novos alertas

### Alert ↔ Hive

```java
// Validação de propriedade no AlertService
if (!hive.getOwner().getId().equals(ownerId)) {
    throw new BadRequestException("Hive does not belong to the owner");
}
```

Relacionamento **Many-to-One** com validação de segurança:

- Usuários só acessam alertas de suas próprias colmeias
- Deleção de colmeia remove todos os alertas associados
- Status da colmeia (ACTIVE/INACTIVE) não afeta alertas existentes

## 7. Implementação das APIs

### Endpoints Disponíveis

#### GET /api/alerts/{alertId}

**Buscar alerta específico por ID**

```bash
curl -X GET \
  'http://localhost:8080/api/alerts/123e4567-e89b-12d3-a456-426614174000' \
  -H 'Authorization: Bearer {token}'
```

**Resposta**:

```json
{
	"id": "123e4567-e89b-12d3-a456-426614174000",
	"type": "TEMPERATURE",
	"timestamp": "2025-07-13T14:30:00",
	"severity": "HIGH",
	"value": 45.5,
	"status": "NEW",
	"hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### GET /api/alerts/hive/{hiveId}

**Listar alertas de uma colmeia com filtros**

```bash
curl -X GET \
  'http://localhost:8080/api/alerts/hive/f47ac10b-58cc-4372-a567-0e02b2c3d479?status=NEW&page=0&size=10' \
  -H 'Authorization: Bearer {token}'
```

**Parâmetros**:

- `status` (opcional): NEW, VIEWED, RESOLVED
- `page`: Número da página (padrão: 0)
- `size`: Itens por página (padrão: 20)

#### PATCH /api/alerts/{alertId}/status/{status}

**Atualizar status de um alerta**

```bash
curl -X PATCH \
  'http://localhost:8080/api/alerts/123e4567-e89b-12d3-a456-426614174000/status/VIEWED' \
  -H 'Authorization: Bearer {token}'
```

**Status válidos**: NEW, VIEWED, RESOLVED

### Códigos de Resposta

- **200 OK**: Operação realizada com sucesso
- **401 Unauthorized**: Token inválido ou expirado
- **403 Forbidden**: Usuário sem permissão para acessar o recurso
- **404 Not Found**: Alerta ou colmeia não encontrados
- **400 Bad Request**: Dados inválidos ou alerta não pertence ao usuário

## 8. Cálculo de Severidade

### Algoritmo de Classificação

```java
private Alert.AlertSeverity calculateSeverity(Alert.AlertType type, Double value, Threshold threshold) {
    double min, max;

    // Define limites baseado no tipo de sensor
    switch (type) {
        case TEMPERATURE: min = threshold.getTemperatureMin(); max = threshold.getTemperatureMax(); break;
        case HUMIDITY: min = threshold.getHumidityMin(); max = threshold.getHumidityMax(); break;
        case CO2: min = threshold.getCo2Min(); max = threshold.getCo2Max(); break;
    }

    // Se está dentro do range, não gera alerta
    if (value >= min && value <= max) return null;

    // Calcula distância proporcional do limite
    double distance = value < min ? min - value : value - max;
    double range = max - min;
    double percentage = (range == 0) ? 100 : (distance / range) * 100;

    // Classifica severidade baseada na porcentagem
    if (percentage <= 10) return Alert.AlertSeverity.LOW;
    else if (percentage <= 30) return Alert.AlertSeverity.MEDIUM;
    else return Alert.AlertSeverity.HIGH;
}
```

### Exemplos de Classificação

**Cenário**: Threshold de temperatura 15°C - 35°C (range = 20°C)

| Valor Medido | Distância | Porcentagem | Severidade |
| ------------ | --------- | ----------- | ---------- |
| 37°C         | +2°C      | 10%         | LOW        |
| 40°C         | +5°C      | 25%         | MEDIUM     |
| 50°C         | +15°C     | 75%         | HIGH       |
| 13°C         | -2°C      | 10%         | LOW        |
| 5°C          | -10°C     | 50%         | HIGH       |

## 9. Gerenciamento via APIs

### Segurança e Autorizações

**Controle de Acesso**:

- Todas as APIs requerem autenticação JWT
- Usuários só acessam alertas de suas próprias colmeias
- Validação dupla: token válido + propriedade da colmeia

**Validações Implementadas**:

```java
// Verificação de propriedade da colmeia
Hive hive = hiveRepositoryPort.findById(hiveId)
    .orElseThrow(() -> new NotFoundException("Hive not found"));

if (!hive.getOwner().getId().equals(ownerId)) {
    throw new BadRequestException("Hive does not belong to the owner");
}
```

### Paginação e Performance

**Consultas Otimizadas**:

- Índices no banco: `(hive_id, status, timestamp)`
- Paginação nativa do Spring Data
- Projeção de dados apenas necessários

**Exemplo de Resposta Paginada**:

```json
{
	"content": [
		{
			"id": "123e4567-e89b-12d3-a456-426614174000",
			"type": "TEMPERATURE",
			"severity": "HIGH",
			"value": 45.5,
			"status": "NEW",
			"timestamp": "2025-07-13T14:30:00",
			"hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
		}
	],
	"totalElements": 25,
	"totalPages": 3,
	"size": 10,
	"number": 0
}
```

## 10. Exemplos de Interação

### Cenário Completo: Da Medição ao Alerta

#### Passo 1: Dispositivo IoT Envia Medição

```bash
curl -X POST http://localhost:8080/api/measurements/iot \
  -H "X-API-Key: hive_12345_api_key_abcdef" \
  -H "Content-Type: application/json" \
  -d '{
    "temperature": 45.5,
    "humidity": 85.0,
    "co2": 1500.0,
    "measuredAt": "2025-01-15T14:30:00"
  }'
```

#### Passo 2: Sistema Verifica Thresholds

```
Threshold configurado:
- Temperatura: 15°C - 35°C
- Umidade: 30% - 80%
- CO2: 300 - 1200 ppm

Medição recebida:
- Temperatura: 45.5°C ❌ (10.5°C acima do limite)
- Umidade: 85.0% ❌ (5% acima do limite)
- CO2: 1500 ppm ❌ (300 ppm acima do limite)
```

#### Passo 3: Alertas Criados Automaticamente

```
🚨 Alert #1: TEMPERATURE
   Severidade: HIGH (52.5% acima do range)
   Valor: 45.5°C
   Status: NEW

🚨 Alert #2: HUMIDITY
   Severidade: LOW (10% acima do range)
   Valor: 85.0%
   Status: NEW

🚨 Alert #3: CO2
   Severidade: MEDIUM (33% acima do range)
   Valor: 1500 ppm
   Status: NEW
```

#### Passo 4: Usuário Consulta Alertas

```bash
GET /api/alerts/hive/f47ac10b-58cc-4372-a567-0e02b2c3d479?status=NEW

Response: 3 alertas NEW criados com timestamp 14:30:00
```

#### Passo 5: Usuário Gerencia Alertas

```bash
# Marca como visualizado
PATCH /api/alerts/123.../status/VIEWED

# Após resolver o problema
PATCH /api/alerts/123.../status/RESOLVED
```

### Integração com Dashboard Frontend

```typescript
// Consultar alertas de alta prioridade
const highPriorityAlerts = await api.get(
	`/alerts/hive/${hiveId}?status=NEW&severity=HIGH`
);

// Atualizar status quando usuário visualiza
const markAsViewed = async (alertId: string) => {
	await api.patch(`/alerts/${alertId}/status/VIEWED`);
};

// Indicadores visuais por severidade
const getSeverityColor = (severity: string) => {
	switch (severity) {
		case "HIGH":
			return "#FF4444";
		case "MEDIUM":
			return "#FF8800";
		case "LOW":
			return "#FFAA00";
	}
};
```

## 11. Possíveis Extensões Futuras

### Notificações em Tempo Real

```java
@EventListener
public void handleNewAlert(AlertCreatedEvent event) {
    // Enviar push notification
    // Enviar email crítico para alertas HIGH
    // Webhook para sistemas externos
}
```

### Alertas Compostos e Padrões

```java
// Detectar padrões: "3 alertas de temperatura em 1 hora"
public class AlertPatternDetector {
    public void analyzePattern(List<Alert> recentAlerts) {
        // Lógica para detectar padrões preocupantes
        // Criar meta-alertas baseados em tendências
    }
}
```

### Machine Learning para Predição

```java
// Predizer alertas baseado em histórico e tendências
public class AlertPredictor {
    public List<PredictedAlert> predictNextHour(Hive hive, List<Measurement> history) {
        // Modelo ML para prever condições futuras
        // Alertas preventivos antes do problema acontecer
    }
}
```

### Integração com Sistemas Externos

```java
// Webhook para sistemas de automação
@RestController
public class AlertWebhookController {
    @PostMapping("/webhook/alerts")
    public void receiveAlert(@RequestBody Alert alert) {
        // Integrar com sistemas de HVAC, irrigação, etc.
        // Automatizar respostas a condições críticas
    }
}
```

### Analytics e Reporting

```java
// Relatórios de tendências de alertas
public class AlertAnalyticsService {
    public AlertTrendReport generateTrendReport(UUID hiveId, Period period) {
        // Frequência de alertas por tipo
        // Horários mais propensos a problemas
        // Correlação entre condições ambientais
    }
}
```

---

Esta documentação cobre todos os aspectos do módulo de Alertas no TechMel. Para dúvidas adicionais ou suporte técnico, consulte os outros módulos (Measurement, Threshold, Hive) ou entre em contato com a equipe de desenvolvimento.
