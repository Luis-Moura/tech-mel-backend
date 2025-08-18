# Documentação Técnica: Módulo de Limites (Thresholds) no TechMel

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura da Solução](#2-arquitetura-da-solução)
3. [Entidades e Relacionamentos](#3-entidades-e-relacionamentos)
4. [Casos de Uso (Use Cases)](#4-casos-de-uso-use-cases)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [Integração entre Módulos](#6-integração-entre-módulos)
7. [Implementação das APIs](#7-implementação-das-apis)
8. [Validações e Regras de Negócio](#8-validações-e-regras-de-negócio)
9. [Configuração e Calibração](#9-configuração-e-calibração)
10. [Exemplos de Interação](#10-exemplos-de-interação)
11. [Possíveis Extensões Futuras](#11-possíveis-extensões-futuras)

## 1. Visão Geral

O módulo de Limites (Thresholds) é o **sistema de configuração central** do TechMel, responsável por definir os **parâmetros ideais de ambiente** para cada colmeia. Este módulo estabelece os limites mínimos e máximos para temperatura, umidade e concentração de CO₂, servindo como base para o **sistema de alertas automatizados**.

### Características Principais

- **Configuração por Colmeia**: Cada colmeia possui seus próprios limites personalizados
- **Três Parâmetros Ambientais**: Temperatura (°C), Umidade (%) e CO₂ (ppm)
- **Validação de Ranges**: Valores dentro de faixas cientificamente válidas
- **Relação 1:1**: Um threshold por colmeia (obrigatório para funcionamento)
- **Integração com Alertas**: Base para cálculo de severidade de alertas
- **Gestão Completa**: APIs para criar, consultar, atualizar limites

### Importância no Ecosistema

O módulo de Thresholds é **fundamental** para o funcionamento do TechMel:

- **Sem threshold configurado**: Sistema não pode gerar alertas
- **Alertas dependem**: Severidade calculada com base na distância dos limites
- **Personalização**: Permite adaptar limites para diferentes regiões/espécies
- **Qualidade dos Dados**: Garante que apenas limites válidos sejam aceitos

## 2. Arquitetura da Solução

### Diagrama de Componentes

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Threshold     │    │   PostgreSQL    │
│   Dashboard     │    │    Service      │    │    Database     │
│                 │    │                 │    │                 │
│ • Configuração  │◄──►│ • Validação     │◄──►│ • threshold     │
│ • Calibração    │    │ • CRUD ops      │    │ • hives         │
│ • Recomendações │    │ • Regras        │    │ • users         │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                       ┌─────────────────┐
                       │   Alert         │
                       │   Service       │
                       │                 │
                       │ • Cálculo       │
                       │   Severidade    │
                       │ • Validação     │
                       │   Limites       │
                       └─────────────────┘
```

### Componentes Principais

1. **ThresholdController**: Endpoints REST para gestão de limites
2. **ThresholdService**: Lógica de negócio e validações
3. **ThresholdRepositoryPort**: Interface para persistência no PostgreSQL
4. **ThresholdMapper**: Conversão entre entidades JPA e objetos de domínio
5. **ThresholdEntity**: Entidade JPA para mapeamento da tabela
6. **Validation Layer**: Validações de ranges e consistência de dados

### Fluxo de Integração

```
┌─────────────┐     configura      ┌─────────────┐     usa para      ┌─────────────┐
│    User     │ ─────────────────► │ Threshold   │ ────────────────► │   Alert     │
│  (Common)   │    via API         │   Service   │   cálculo de      │  Service    │
│             │                    │             │   severidade      │             │
└─────────────┘                    └─────────────┘                   └─────────────┘
                                          │
                                          ▼
                                  ┌─────────────┐
                                  │    Hive     │
                                  │   1:1 rel   │
                                  │             │
                                  └─────────────┘
```

## 3. Entidades e Relacionamentos

### Entidade Threshold

```java
public class Threshold {
    private UUID id;                    // Identificador único
    private Double temperatureMin;      // Temperatura mínima (°C)
    private Double temperatureMax;      // Temperatura máxima (°C)
    private Double humidityMin;         // Umidade mínima (%)
    private Double humidityMax;         // Umidade máxima (%)
    private Double co2Min;             // CO2 mínimo (ppm)
    private Double co2Max;             // CO2 máximo (ppm)
    private Hive hive;                 // Relação 1:1 com colmeia
}
```

### Relacionamentos

- **Threshold ↔ Hive**: Relacionamento **One-to-One** obrigatório
- **Cascade Delete**: Quando colmeia é deletada, threshold também é removido
- **Unique Constraint**: Um threshold por colmeia (unique index em hive_id)
- **Foreign Key**: hive_id NOT NULL com ON DELETE CASCADE

### Regras de Integridade

```sql
-- Estrutura da tabela
CREATE TABLE threshold (
    id UUID PRIMARY KEY,
    temperature_min DOUBLE PRECISION NOT NULL,
    temperature_max DOUBLE PRECISION NOT NULL,
    humidity_min DOUBLE PRECISION NOT NULL,
    humidity_max DOUBLE PRECISION NOT NULL,
    co2_min DOUBLE PRECISION NOT NULL,
    co2_max DOUBLE PRECISION NOT NULL,
    hive_id UUID NOT NULL UNIQUE,

    FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE CASCADE,

    -- Constraints de validação
    CHECK (temperature_min >= -50 AND temperature_min <= 60),
    CHECK (temperature_max >= -50 AND temperature_max <= 60),
    CHECK (temperature_min < temperature_max),

    CHECK (humidity_min >= 0 AND humidity_min <= 100),
    CHECK (humidity_max >= 0 AND humidity_max <= 100),
    CHECK (humidity_min < humidity_max),

    CHECK (co2_min >= 0 AND co2_min <= 5000),
    CHECK (co2_max >= 0 AND co2_max <= 5000),
    CHECK (co2_min < co2_max)
);
```

### Estados e Lifecycle

```
┌─────────────┐    CREATE     ┌─────────────┐    UPDATE     ┌─────────────┐
│   Hive      │ ──────────────► │ Threshold   │ ──────────────► │ Threshold   │
│   Criada    │    (required)  │   Inicial   │   (opcional)   │  Atualizado │
│             │                │             │                │             │
└─────────────┘                └─────────────┘                └─────────────┘
                                       │
                                   DELETE
                                       ▼
                               ┌─────────────┐
                               │  Threshold  │
                               │   Removed   │
                               │ (with hive) │
                               └─────────────┘
```

## 4. Casos de Uso (Use Cases)

### Interface ThresholdUseCase

```java
public interface ThresholdUseCase {
    Threshold createThreshold(CreateThresholdRequest request, UUID ownerId);
    Threshold getThresholdById(UUID thresholdId, UUID ownerId);
    Threshold getThresholdByHiveId(UUID hiveId, UUID ownerId);
    void updateThreshold(UUID thresholdId, CreateThresholdRequest request, UUID ownerId);
}
```

### Casos de Uso Detalhados

#### UC01: Criação de Threshold

- **Ator**: Usuário Common/Technician/Admin
- **Pré-condição**: Usuário proprietário da colmeia, threshold não existe
- **Fluxo Principal**:
  1. Usuário envia dados de threshold via POST
  2. Sistema valida propriedade da colmeia
  3. Sistema verifica se threshold já existe para a colmeia
  4. Sistema valida ranges dos valores (temperatura, umidade, CO2)
  5. Sistema valida consistência (min < max para cada parâmetro)
  6. Sistema cria e persiste threshold
  7. Retorna dados do threshold criado

#### UC02: Consulta Threshold por ID

- **Ator**: Usuário autenticado
- **Pré-condição**: Threshold existe e usuário é proprietário
- **Fluxo Principal**:
  1. Cliente requisita threshold por ID
  2. Sistema verifica existência do threshold
  3. Sistema valida propriedade através da colmeia
  4. Retorna dados completos do threshold

#### UC03: Consulta Threshold por Colmeia

- **Ator**: Usuário autenticado
- **Pré-condição**: Usuário proprietário da colmeia
- **Fluxo Principal**:
  1. Cliente requisita threshold de uma colmeia específica
  2. Sistema valida propriedade da colmeia
  3. Sistema busca threshold associado
  4. Retorna dados do threshold ou erro 404 se não configurado

#### UC04: Atualização de Threshold

- **Ator**: Usuário proprietário
- **Pré-condição**: Threshold existe e usuário tem permissão
- **Fluxo Principal**:
  1. Usuário envia novos valores via PUT
  2. Sistema valida propriedade do threshold
  3. Sistema valida novos ranges e consistência
  4. Sistema atualiza todos os campos
  5. Sistema persiste alterações
  6. Novos alertas usarão os limites atualizados

#### UC05: Validação durante Criação de Alert

- **Ator**: Sistema (AlertService)
- **Pré-condição**: Nova medição recebida
- **Fluxo Principal**:
  1. AlertService recebe medição
  2. Sistema busca threshold por hiveId
  3. Sistema compara valores medidos com limites configurados
  4. Sistema calcula severidade baseada na distância dos limites
  5. Sistema cria alertas para valores fora dos ranges

## 5. Fluxo de Dados

### Fluxo de Criação de Threshold

```
1. Frontend Dashboard
   │ POST /api/hives/thresholds
   │ Body: {temperatureMin, temperatureMax, humidityMin, humidityMax, co2Min, co2Max, hiveId}
   ▼
2. ThresholdController
   │ @Valid @RequestBody CreateThresholdRequest
   │ authenticationUtil.getCurrentUserId()
   ▼
3. ThresholdService.createThreshold()
   │ ├─ hiveRepositoryPort.findById(request.hiveId())
   │ ├─ validate hive.getOwner().getId().equals(ownerId)
   │ ├─ thresholdRepositoryPort.findByHiveId(hiveId) // deve retornar empty
   │ ├─ validate ranges (temperature: -50~60, humidity: 0~100, co2: 0~5000)
   │ ├─ validate consistency (min < max for each parameter)
   │ ├─ build Threshold object
   │ └─ thresholdRepositoryPort.save(threshold)
   ▼
4. ThresholdRepositoryAdapter
   │ ├─ thresholdMapper.toEntity(threshold)
   │ ├─ repository.save(thresholdEntity)
   │ └─ thresholdMapper.toDomain(savedEntity)
   ▼
5. PostgreSQL
   │ INSERT INTO threshold VALUES (...)
   │ UNIQUE constraint on hive_id
   │ CHECK constraints on ranges
   ▼
6. Response HTTP 200
   │ ThresholdResponse with all data + hiveId
```

### Fluxo de Consulta e Uso em Alertas

```
1. AlertService (internal call)
   │ alertUseCase.saveAlert(measurement, hive, timestamp)
   ▼
2. AlertService.saveAlert()
   │ thresholdRepositoryPort.findByHiveId(hive.getId())
   │ .orElseThrow("Threshold not configured")
   ▼
3. Comparação e Cálculo
   │ ├─ if (temperature < threshold.getTemperatureMin() || temperature > threshold.getTemperatureMax())
   │ │   └─ create TEMPERATURE alert with calculated severity
   │ ├─ if (humidity < threshold.getHumidityMin() || humidity > threshold.getHumidityMax())
   │ │   └─ create HUMIDITY alert with calculated severity
   │ └─ if (co2 < threshold.getCo2Min() || co2 > threshold.getCo2Max())
   │     └─ create CO2 alert with calculated severity
   ▼
4. Cálculo de Severidade
   │ distance = value < min ? min - value : value - max
   │ range = max - min
   │ percentage = (distance / range) * 100
   │
   │ if (percentage <= 10%) → AlertSeverity.LOW
   │ else if (percentage <= 30%) → AlertSeverity.MEDIUM
   │ else → AlertSeverity.HIGH
   ▼
5. Persistência de Alertas
   │ alertRepositoryPort.save(alert) for each violation
```

### Fluxo de Atualização

```
1. User Interface
   │ PUT /api/hives/thresholds/{thresholdId}
   │ Body: {updated values for all parameters}
   ▼
2. Validation & Update
   │ ├─ find threshold by ID
   │ ├─ validate ownership through hive relationship
   │ ├─ validate new ranges and consistency
   │ ├─ update all fields (full replacement)
   │ └─ save updated threshold
   ▼
3. Impact on Future Operations
   │ ├─ Existing alerts: NOT affected
   │ ├─ New measurements: Will use updated limits
   │ └─ Alert calculations: Use new severity boundaries
```

## 6. Integração entre Módulos

### Threshold → Alert (Dependência Crítica)

```java
// AlertService requer threshold para funcionamento
Threshold threshold = thresholdRepositoryPort.findByHiveId(hive.getId())
    .orElseThrow(() -> new NotFoundException("Threshold not configured"));
```

**Características da Integração**:

- **Dependência Obrigatória**: Alertas não podem ser criados sem threshold
- **Acoplamento Forte**: AlertService sempre consulta threshold atual
- **Performance**: Consulta otimizada com índice em hive_id
- **Consistência**: Alertas sempre usam limites mais atuais

### Threshold → Hive (Relacionamento 1:1)

```java
// Validação de propriedade e existência
Hive hive = hiveRepositoryPort.findById(request.hiveId())
    .orElseThrow(() -> new NotFoundException("Hive not found"));

if (!hive.getOwner().getId().equals(ownerId)) {
    throw new NotFoundException("Hive does not belong to the specified owner");
}
```

**Relacionamento**:

- **Cardinalidade**: 1:1 (um threshold por colmeia)
- **Obrigatoriedade**: Threshold é opcional, mas recomendado
- **Segurança**: Validação de propriedade através da colmeia
- **Lifecycle**: Threshold é deletado junto com a colmeia

### Threshold → Measurement (Indireta via Alert)

```java
// Fluxo: Measurement → Alert → Threshold
public void registerMeasurement(String apiKey, CreateMeasurementRequest request) {
    // ... salvar medição no Redis ...
    alertUseCase.saveAlert(measurement, hive, request.measuredAt());
    // Aqui o AlertService usa o threshold para determinar alertas
}
```

**Integração Indireta**:

- Medições não dependem diretamente de thresholds
- Thresholds são consultados apenas quando alertas são avaliados
- Permite flexibilidade: colmeias podem funcionar sem threshold (sem alertas)

## 7. Implementação das APIs

### Endpoints Disponíveis

#### POST /api/hives/thresholds

**Criar limites para uma colmeia**

```bash
curl -X POST http://localhost:8080/api/hives/thresholds \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "temperatureMin": 18.0,
    "temperatureMax": 35.0,
    "humidityMin": 40.0,
    "humidityMax": 80.0,
    "co2Min": 300.0,
    "co2Max": 1200.0,
    "hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }'
```

**Validações**:

- `temperatureMin/Max`: -50°C a 60°C
- `humidityMin/Max`: 0% a 100%
- `co2Min/Max`: 0 a 5000 ppm
- `min < max` para cada parâmetro
- `hiveId` deve existir e pertencer ao usuário
- Colmeia não pode ter threshold já configurado

**Resposta**:

```json
{
	"id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
	"temperatureMin": 18.0,
	"temperatureMax": 35.0,
	"humidityMin": 40.0,
	"humidityMax": 80.0,
	"co2Min": 300.0,
	"co2Max": 1200.0,
	"hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### GET /api/hives/thresholds/{thresholdId}

**Buscar threshold por ID**

```bash
curl -X GET \
  'http://localhost:8080/api/hives/thresholds/d290f1ee-6c54-4b01-90e6-d701748f0851' \
  -H 'Authorization: Bearer {token}'
```

#### GET /api/hives/thresholds/hive/{hiveId}

**Buscar threshold de uma colmeia específica**

```bash
curl -X GET \
  'http://localhost:8080/api/hives/thresholds/hive/f47ac10b-58cc-4372-a567-0e02b2c3d479' \
  -H 'Authorization: Bearer {token}'
```

#### PUT /api/hives/thresholds/{thresholdId}

**Atualizar limites existentes**

```bash
curl -X PUT \
  'http://localhost:8080/api/hives/thresholds/d290f1ee-6c54-4b01-90e6-d701748f0851' \
  -H 'Authorization: Bearer {token}' \
  -H 'Content-Type: application/json' \
  -d '{
    "temperatureMin": 20.0,
    "temperatureMax": 32.0,
    "humidityMin": 45.0,
    "humidityMax": 75.0,
    "co2Min": 350.0,
    "co2Max": 1000.0,
    "hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }'
```

### Códigos de Resposta

- **200 OK**: Operação realizada com sucesso
- **204 No Content**: Atualização realizada com sucesso
- **400 Bad Request**: Dados inválidos, ranges incorretos, ou threshold já existe
- **401 Unauthorized**: Token inválido ou expirado
- **403 Forbidden**: Usuário sem permissão
- **404 Not Found**: Threshold, colmeia não encontrados, ou usuário não é proprietário

### Validações de Request

```java
@Schema(description = "Dados para criação de limites de sensores da colmeia")
public record CreateThresholdRequest(
    @Schema(description = "Temperatura mínima permitida em graus Celsius",
            minimum = "-50", maximum = "60")
    @NotNull
    Double temperatureMin,

    @Schema(description = "Temperatura máxima permitida em graus Celsius",
            minimum = "-50", maximum = "60")
    @NotNull
    Double temperatureMax,

    // ... outros campos com validações similares

    @Schema(description = "Identificador da colmeia")
    @NotNull
    UUID hiveId
) {
    // Validações customizadas podem ser adicionadas aqui
}
```

## 8. Validações e Regras de Negócio

### Validações de Entrada

#### Ranges Científicos

```java
// Validações baseadas em literatura científica sobre apicultura
public class ThresholdValidationService {

    private static final double TEMP_MIN_LIMIT = -50.0;  // Limite técnico do sensor
    private static final double TEMP_MAX_LIMIT = 60.0;   // Limite técnico do sensor
    private static final double HUMIDITY_MIN_LIMIT = 0.0;
    private static final double HUMIDITY_MAX_LIMIT = 100.0;
    private static final double CO2_MIN_LIMIT = 0.0;
    private static final double CO2_MAX_LIMIT = 5000.0;  // Limite técnico do sensor

    public void validateTemperature(Double min, Double max) {
        validateRange(min, max, TEMP_MIN_LIMIT, TEMP_MAX_LIMIT, "temperature");

        // Validação específica: range mínimo de 5°C para ser útil
        if (max - min < 5.0) {
            throw new BadRequestException("Temperature range must be at least 5°C");
        }
    }

    public void validateHumidity(Double min, Double max) {
        validateRange(min, max, HUMIDITY_MIN_LIMIT, HUMIDITY_MAX_LIMIT, "humidity");

        // Validação específica: range mínimo de 10% para ser útil
        if (max - min < 10.0) {
            throw new BadRequestException("Humidity range must be at least 10%");
        }
    }

    public void validateCo2(Double min, Double max) {
        validateRange(min, max, CO2_MIN_LIMIT, CO2_MAX_LIMIT, "co2");

        // Validação específica: range mínimo de 100 ppm
        if (max - min < 100.0) {
            throw new BadRequestException("CO2 range must be at least 100 ppm");
        }
    }
}
```

#### Regras de Consistência

```java
// Implementação no ThresholdService
@Override
public Threshold createThreshold(CreateThresholdRequest request, UUID ownerId) {
    // 1. Validar propriedade da colmeia
    Hive hive = validateHiveOwnership(request.hiveId(), ownerId);

    // 2. Verificar se threshold já existe
    Optional<Threshold> existingThreshold = thresholdRepositoryPort.findByHiveId(request.hiveId());
    if (existingThreshold.isPresent()) {
        throw new BadRequestException("Threshold already exists for this hive.");
    }

    // 3. Validar ranges individuais
    validateThresholdRanges(request);

    // 4. Validar consistência cross-parameter (se necessário)
    validateCrossParameterConsistency(request);

    // 5. Criar e salvar
    Threshold threshold = buildThreshold(request, hive);
    return thresholdRepositoryPort.save(threshold);
}

private void validateThresholdRanges(CreateThresholdRequest request) {
    ThresholdValidationService.validateTemperature(request.temperatureMin(), request.temperatureMax());
    ThresholdValidationService.validateHumidity(request.humidityMin(), request.humidityMax());
    ThresholdValidationService.validateCo2(request.co2Min(), request.co2Max());
}
```

### Regras de Negócio Específicas

#### Threshold por Colmeia

```java
// Garantir apenas um threshold por colmeia
Optional<Threshold> existingThreshold = thresholdRepositoryPort.findByHiveId(hiveId);
if (existingThreshold.isPresent()) {
    throw new BadRequestException("Threshold already exists for this hive. Use UPDATE to modify.");
}
```

#### Validação de Propriedade

```java
// Usuário só pode configurar thresholds de suas próprias colmeias
private Hive validateHiveOwnership(UUID hiveId, UUID ownerId) {
    Hive hive = hiveRepositoryPort.findById(hiveId)
        .orElseThrow(() -> new NotFoundException("Hive not found"));

    if (!hive.getOwner().getId().equals(ownerId)) {
        throw new NotFoundException("Hive does not belong to the specified owner");
    }

    return hive;
}
```

#### Impacto em Alertas Existentes

```java
// Atualização de threshold NÃO afeta alertas já criados
// Apenas novos alertas usarão os limites atualizados
@Override
public void updateThreshold(UUID thresholdId, CreateThresholdRequest request, UUID ownerId) {
    Threshold threshold = findAndValidateThreshold(thresholdId, ownerId);

    // Atualizar todos os campos
    updateThresholdFields(threshold, request);

    // Salvar (alertas futuros usarão novos limites)
    thresholdRepositoryPort.save(threshold);

    log.info("Threshold updated for hive {}. New measurements will use updated limits.",
             threshold.getHive().getId());
}
```

## 9. Configuração e Calibração

### Valores Recomendados por Região

#### Clima Temperado (Europa, Norte dos EUA)

```json
{
	"region": "temperate",
	"recommended": {
		"temperatureMin": 15.0,
		"temperatureMax": 35.0,
		"humidityMin": 40.0,
		"humidityMax": 80.0,
		"co2Min": 300.0,
		"co2Max": 1000.0
	},
	"critical": {
		"temperatureMin": 10.0,
		"temperatureMax": 40.0,
		"humidityMin": 30.0,
		"humidityMax": 90.0,
		"co2Min": 200.0,
		"co2Max": 1500.0
	}
}
```

#### Clima Tropical (Brasil, Sudeste Asiático)

```json
{
	"region": "tropical",
	"recommended": {
		"temperatureMin": 20.0,
		"temperatureMax": 32.0,
		"humidityMin": 50.0,
		"humidityMax": 85.0,
		"co2Min": 300.0,
		"co2Max": 1200.0
	},
	"critical": {
		"temperatureMin": 18.0,
		"temperatureMax": 38.0,
		"humidityMin": 40.0,
		"humidityMax": 95.0,
		"co2Min": 250.0,
		"co2Max": 1800.0
	}
}
```

### Sistema de Recomendações

```java
// Futuro: Sistema de recomendações inteligentes
@Service
public class ThresholdRecommendationService {

    public ThresholdRecommendation getRecommendation(UUID hiveId) {
        Hive hive = hiveRepositoryPort.findById(hiveId).orElseThrow();

        // Fatores considerados:
        Location location = geoService.getLocation(hive.getAddress());
        Climate climate = climateService.getClimate(location);
        Season season = Season.current();
        BeeTybe beeType = hive.getBeeType(); // futuro

        // Algoritmo de recomendação
        return RecommendationAlgorithm.calculate(location, climate, season, beeType);
    }

    public List<ThresholdAlert> validateCurrentThreshold(UUID hiveId) {
        Threshold current = thresholdRepositoryPort.findByHiveId(hiveId).orElse(null);
        if (current == null) return List.of();

        ThresholdRecommendation recommended = getRecommendation(hiveId);

        List<ThresholdAlert> alerts = new ArrayList<>();

        // Verificar se atual está muito diferente do recomendado
        if (Math.abs(current.getTemperatureMin() - recommended.getTemperatureMin()) > 5.0) {
            alerts.add(new ThresholdAlert("TEMPERATURE_MIN_DEVIATION",
                "Current minimum temperature is significantly different from recommended"));
        }

        return alerts;
    }
}
```

### Calibração Assistida

```java
// Futuro: Calibração baseada em dados históricos
@Service
public class ThresholdCalibrationService {

    public CalibrationSuggestion analyzeBehavior(UUID hiveId, Period period) {
        // Analisar padrões de alerta
        List<Alert> alerts = alertRepositoryPort.findByHiveAndPeriod(hiveId, period);

        // Analisar distribuição de medições
        List<DailyMeasurementAverage> measurements =
            dailyMeasurementAverageRepositoryPort.findByHiveAndPeriod(hiveId, period);

        return CalibrationSuggestion.builder()
            .alertFrequency(calculateAlertFrequency(alerts))
            .measurementDistribution(analyzeMeasurementDistribution(measurements))
            .suggestedAdjustments(calculateAdjustments(alerts, measurements))
            .confidence(calculateConfidence())
            .build();
    }

    public List<ThresholdAdjustment> getSuggestedAdjustments(UUID hiveId) {
        CalibrationSuggestion suggestion = analyzeBehavior(hiveId, Period.ofDays(30));

        List<ThresholdAdjustment> adjustments = new ArrayList<>();

        // Se muitos alertas LOW, talvez limites estejam muito sensíveis
        if (suggestion.getAlertFrequency().getLowAlerts() > 10) {
            adjustments.add(ThresholdAdjustment.builder()
                .parameter("temperature")
                .type("EXPAND_RANGE")
                .suggestion("Consider expanding temperature range by ±2°C")
                .confidence(0.75)
                .build());
        }

        return adjustments;
    }
}
```

## 10. Exemplos de Interação

### Cenário 1: Setup Inicial de uma Colmeia

#### Passo 1: Usuário Cria Colmeia

```bash
# Primeiro criar a colmeia (via Hive API)
POST /api/technician/hives
{
  "name": "Colmeia Jardim",
  "location": "Rua das Flores, 123 - São Paulo/SP",
  "ownerId": "user-uuid-123"
}

# Response: hiveId = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
```

#### Passo 2: Configurar Thresholds

```bash
# Configurar limites baseados no clima local (São Paulo)
POST /api/hives/thresholds
{
  "temperatureMin": 18.0,
  "temperatureMax": 32.0,
  "humidityMin": 45.0,
  "humidityMax": 80.0,
  "co2Min": 300.0,
  "co2Max": 1200.0,
  "hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### Passo 3: Sistema Valida e Cria

```
2025-01-15 10:00:01.123 INFO  ThresholdService - Creating threshold for hive f47ac10b-...
2025-01-15 10:00:01.124 INFO  ThresholdService - Validating hive ownership...
2025-01-15 10:00:01.125 INFO  ThresholdService - Checking for existing threshold...
2025-01-15 10:00:01.126 INFO  ThresholdService - Validating ranges: temp(18.0-32.0), humidity(45.0-80.0), co2(300.0-1200.0)
2025-01-15 10:00:01.127 INFO  ThresholdService - Threshold created successfully with ID d290f1ee-...
```

#### Passo 4: Primeira Medição Usando Threshold

```bash
# Dispositivo IoT envia primeira medição
POST /api/measurements/iot
Headers: X-API-Key: hive_garden_api_key_xyz
{
  "temperature": 35.5,  # ❌ Acima do limite (32.0°C)
  "humidity": 85.0,     # ❌ Acima do limite (80.0%)
  "co2": 400.0,         # ✅ Dentro do limite
  "measuredAt": "2025-01-15T14:30:00"
}

# Sistema automaticamente:
# 1. Salva medição no Redis
# 2. Consulta threshold da colmeia
# 3. Detecta violações em temperatura e umidade
# 4. Cria 2 alertas automaticamente
```

### Cenário 2: Ajuste de Thresholds Baseado em Experiência

#### Situação: Muitos Alertas de Baixa Severidade

```bash
# Usuário consulta alertas dos últimos 7 dias
GET /api/alerts/hive/f47ac10b-58cc-4372-a567-0e02b2c3d479?page=0&size=50

# Response mostra 15 alertas LOW de temperatura
# Todos na faixa de 32.1°C - 33.5°C
```

#### Decisão: Ajustar Limite Superior

```bash
# Usuário consulta threshold atual
GET /api/hives/thresholds/hive/f47ac10b-58cc-4372-a567-0e02b2c3d479

# Response: temperatureMax = 32.0

# Usuário decide aumentar para 34.0°C baseado na experiência local
PUT /api/hives/thresholds/d290f1ee-6c54-4b01-90e6-d701748f0851
{
  "temperatureMin": 18.0,
  "temperatureMax": 34.0,  # ← Aumentado de 32.0 para 34.0
  "humidityMin": 45.0,
  "humidityMax": 80.0,
  "co2Min": 300.0,
  "co2Max": 1200.0,
  "hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
}
```

#### Resultado: Redução de Alertas Falsos

```
2025-01-22 15:30:00 INFO  ThresholdService - Threshold updated for hive f47ac10b-...
2025-01-22 15:30:00 INFO  ThresholdService - New temperature range: 18.0°C - 34.0°C

# Próximas medições de 32.5°C, 33.2°C não geram mais alertas
# Apenas temperaturas > 34.0°C geram alertas
```

### Cenário 3: Integração Frontend com Dashboard

#### Componente de Configuração de Threshold

```typescript
interface ThresholdFormProps {
	hiveId: string;
	existingThreshold?: Threshold;
	onSave: (threshold: Threshold) => void;
}

const ThresholdConfigForm: React.FC<ThresholdFormProps> = ({
	hiveId,
	existingThreshold,
	onSave,
}) => {
	const [formData, setFormData] = useState({
		temperatureMin: existingThreshold?.temperatureMin ?? 18.0,
		temperatureMax: existingThreshold?.temperatureMax ?? 32.0,
		humidityMin: existingThreshold?.humidityMin ?? 45.0,
		humidityMax: existingThreshold?.humidityMax ?? 80.0,
		co2Min: existingThreshold?.co2Min ?? 300.0,
		co2Max: existingThreshold?.co2Max ?? 1200.0,
	});

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();

		try {
			const payload = { ...formData, hiveId };

			let response;
			if (existingThreshold) {
				// Atualizar threshold existente
				response = await api.put(
					`/hives/thresholds/${existingThreshold.id}`,
					payload
				);
			} else {
				// Criar novo threshold
				response = await api.post("/hives/thresholds", payload);
			}

			onSave(response.data);
			toast.success("Limites configurados com sucesso!");
		} catch (error) {
			if (error.response?.status === 400) {
				toast.error("Valores inválidos. Verifique os ranges permitidos.");
			} else {
				toast.error("Erro ao salvar configurações.");
			}
		}
	};

	return (
		<form onSubmit={handleSubmit} className="threshold-form">
			<h3>Configurar Limites da Colmeia</h3>

			<div className="parameter-group">
				<h4>🌡️ Temperatura (°C)</h4>
				<div className="range-inputs">
					<input
						type="number"
						placeholder="Mínima"
						value={formData.temperatureMin}
						onChange={(e) =>
							setFormData({
								...formData,
								temperatureMin: Number(e.target.value),
							})
						}
						min="-50"
						max="60"
						step="0.1"
						required
					/>
					<span>até</span>
					<input
						type="number"
						placeholder="Máxima"
						value={formData.temperatureMax}
						onChange={(e) =>
							setFormData({
								...formData,
								temperatureMax: Number(e.target.value),
							})
						}
						min="-50"
						max="60"
						step="0.1"
						required
					/>
				</div>
			</div>

			<div className="parameter-group">
				<h4>💧 Umidade (%)</h4>
				<div className="range-inputs">
					<input
						type="number"
						placeholder="Mínima"
						value={formData.humidityMin}
						onChange={(e) =>
							setFormData({ ...formData, humidityMin: Number(e.target.value) })
						}
						min="0"
						max="100"
						step="0.1"
						required
					/>
					<span>até</span>
					<input
						type="number"
						placeholder="Máxima"
						value={formData.humidityMax}
						onChange={(e) =>
							setFormData({ ...formData, humidityMax: Number(e.target.value) })
						}
						min="0"
						max="100"
						step="0.1"
						required
					/>
				</div>
			</div>

			<div className="parameter-group">
				<h4>💨 CO₂ (ppm)</h4>
				<div className="range-inputs">
					<input
						type="number"
						placeholder="Mínimo"
						value={formData.co2Min}
						onChange={(e) =>
							setFormData({ ...formData, co2Min: Number(e.target.value) })
						}
						min="0"
						max="5000"
						step="1"
						required
					/>
					<span>até</span>
					<input
						type="number"
						placeholder="Máximo"
						value={formData.co2Max}
						onChange={(e) =>
							setFormData({ ...formData, co2Max: Number(e.target.value) })
						}
						min="0"
						max="5000"
						step="1"
						required
					/>
				</div>
			</div>

			<div className="form-actions">
				<button type="button" onClick={() => loadRecommendedValues()}>
					Usar Valores Recomendados
				</button>
				<button type="submit" className="primary">
					{existingThreshold ? "Atualizar" : "Criar"} Limites
				</button>
			</div>
		</form>
	);
};
```

#### Dashboard com Status de Threshold

```typescript
const HiveThresholdStatus: React.FC<{ hiveId: string }> = ({ hiveId }) => {
	const [threshold, setThreshold] = useState<Threshold | null>(null);
	const [latestMeasurement, setLatestMeasurement] =
		useState<Measurement | null>(null);

	useEffect(() => {
		const fetchData = async () => {
			try {
				// Buscar threshold configurado
				const thresholdResponse = await api.get(
					`/hives/thresholds/hive/${hiveId}`
				);
				setThreshold(thresholdResponse.data);

				// Buscar última medição
				const measurementResponse = await api.get(
					`/measurements/latest/${hiveId}`
				);
				setLatestMeasurement(measurementResponse.data.latestMeasurement);
			} catch (error) {
				if (error.response?.status === 404) {
					// Threshold não configurado
					setThreshold(null);
				}
			}
		};

		fetchData();
	}, [hiveId]);

	if (!threshold) {
		return (
			<div className="threshold-warning">
				⚠️ Limites não configurados
				<button onClick={() => openThresholdConfig(hiveId)}>
					Configurar Agora
				</button>
			</div>
		);
	}

	const getParameterStatus = (value: number, min: number, max: number) => {
		if (value < min) return { status: "low", icon: "🔽" };
		if (value > max) return { status: "high", icon: "🔺" };
		return { status: "ok", icon: "✅" };
	};

	return (
		<div className="threshold-status">
			<h4>Status dos Limites</h4>

			{latestMeasurement && (
				<div className="parameters-status">
					<div className="parameter">
						<span>🌡️ Temperatura</span>
						<span
							className={`value ${
								getParameterStatus(
									latestMeasurement.temperature,
									threshold.temperatureMin,
									threshold.temperatureMax
								).status
							}`}
						>
							{latestMeasurement.temperature}°C
							{
								getParameterStatus(
									latestMeasurement.temperature,
									threshold.temperatureMin,
									threshold.temperatureMax
								).icon
							}
						</span>
						<span className="range">
							({threshold.temperatureMin}°C - {threshold.temperatureMax}°C)
						</span>
					</div>

					{/* Repetir para humidity e co2 */}
				</div>
			)}

			<button onClick={() => openThresholdConfig(hiveId, threshold)}>
				Ajustar Limites
			</button>
		</div>
	);
};
```

## 11. Possíveis Extensões Futuras

### Sistema de Profiles de Threshold

```java
// Profiles pré-configurados para diferentes cenários
@Entity
public class ThresholdProfile {
    private UUID id;
    private String name;              // "Tropical Standard", "Winter Mode", etc.
    private String description;
    private String region;            // "tropical", "temperate", "arid"
    private Season applicableSeason;  // SPRING, SUMMER, AUTUMN, WINTER
    private Double temperatureMin;
    private Double temperatureMax;
    private Double humidityMin;
    private Double humidityMax;
    private Double co2Min;
    private Double co2Max;
    private boolean isDefault;
    private UUID createdBy;          // Admin/Expert que criou
}

@Service
public class ThresholdProfileService {

    public List<ThresholdProfile> getRecommendedProfiles(Location location, Season season) {
        // Retornar profiles adequados para localização e época
    }

    public Threshold applyProfile(UUID hiveId, UUID profileId, UUID ownerId) {
        // Aplicar profile existente a uma colmeia
        ThresholdProfile profile = findProfile(profileId);
        return createThresholdFromProfile(hiveId, profile, ownerId);
    }
}
```

### Threshold Dinâmico Baseado em Condições

```java
// Thresholds que se ajustam automaticamente
@Entity
public class DynamicThreshold extends Threshold {
    private boolean enableSeasonalAdjustment;
    private boolean enableWeatherAdjustment;
    private boolean enableTimeOfDayAdjustment;

    // Ajustes sazonais
    private Double summerTempOffset;      // +2°C no verão
    private Double winterTempOffset;      // -3°C no inverno

    // Ajustes por horário
    private Double dayTimeHumidityOffset; // Dia: -5%
    private Double nightHumidityOffset;   // Noite: +5%
}

@Service
public class DynamicThresholdService {

    @Scheduled(fixedRate = 3600000) // A cada hora
    public void adjustDynamicThresholds() {
        List<DynamicThreshold> dynamicThresholds = findAllDynamicThresholds();

        for (DynamicThreshold threshold : dynamicThresholds) {
            Threshold adjusted = calculateAdjustedThreshold(threshold);
            updateThresholdIfChanged(threshold, adjusted);
        }
    }

    private Threshold calculateAdjustedThreshold(DynamicThreshold base) {
        double tempMin = base.getTemperatureMin();
        double tempMax = base.getTemperatureMax();

        // Ajuste sazonal
        if (base.isEnableSeasonalAdjustment()) {
            Season current = Season.current();
            tempMin += getSeasonalOffset(current, base);
            tempMax += getSeasonalOffset(current, base);
        }

        // Ajuste climático (integração com API de clima)
        if (base.isEnableWeatherAdjustment()) {
            WeatherCondition weather = weatherService.getCurrentWeather(base.getHive().getLocation());
            tempMin += getWeatherOffset(weather);
            tempMax += getWeatherOffset(weather);
        }

        return Threshold.builder()
            .temperatureMin(tempMin)
            .temperatureMax(tempMax)
            // ... outros campos
            .build();
    }
}
```

### Machine Learning para Otimização

```java
// ML para sugerir thresholds ótimos
@Service
public class ThresholdOptimizationService {

    public ThresholdOptimizationResult optimizeThreshold(UUID hiveId, Period period) {
        // Coletar dados históricos
        List<Alert> alerts = alertRepository.findByHiveAndPeriod(hiveId, period);
        List<DailyMeasurementAverage> measurements = measurementRepository.findByHiveAndPeriod(hiveId, period);

        // Análise de performance do threshold atual
        ThresholdPerformance current = analyzeCurrentPerformance(alerts, measurements);

        // Simulações com diferentes thresholds
        List<ThresholdSimulation> simulations = runSimulations(alerts, measurements);

        // Threshold ótimo baseado em critérios:
        // - Minimizar alertas falsos positivos
        // - Maximizar detecção de problemas reais
        // - Balancear sensibilidade vs. especificidade
        Threshold optimal = findOptimalThreshold(simulations);

        return ThresholdOptimizationResult.builder()
            .currentPerformance(current)
            .recommendedThreshold(optimal)
            .expectedImprovement(calculateImprovement(current, optimal))
            .confidence(calculateConfidence())
            .build();
    }

    public List<ThresholdAdjustmentSuggestion> getSmartSuggestions(UUID hiveId) {
        // Sugestões baseadas em:
        // - Padrões de alerta
        // - Comparação com colmeias similares
        // - Tendências sazonais
        // - Feedback do usuário (alertas marcados como falsos positivos)

        return List.of(
            ThresholdAdjustmentSuggestion.builder()
                .parameter("temperature")
                .currentRange("18.0 - 32.0")
                .suggestedRange("20.0 - 34.0")
                .reason("Reduce false positives by 40% based on last 30 days")
                .confidence(0.85)
                .impact("40% fewer low-severity alerts")
                .build()
        );
    }
}
```

### Threshold Colaborativo

```java
// Sistema colaborativo para compartilhar thresholds
@Entity
public class CommunityThreshold {
    private UUID id;
    private String name;
    private String description;
    private Region region;
    private ClimateType climate;
    private int usageCount;        // Quantas colmeias usam
    private double averageRating;  // Rating dos usuários
    private List<ThresholdReview> reviews;
    private UUID contributedBy;
    private LocalDateTime createdAt;
    private boolean isVerified;    // Verificado por especialista
}

@Service
public class CommunityThresholdService {

    public List<CommunityThreshold> findSimilarThresholds(UUID hiveId) {
        Hive hive = hiveRepository.findById(hiveId).orElseThrow();
        Location location = geoService.getLocation(hive.getAddress());

        return communityThresholdRepository.findByRegionAndClimate(
            location.getRegion(),
            location.getClimateType()
        );
    }

    public void contributeThreshold(UUID hiveId, UUID ownerId, String description) {
        Threshold threshold = thresholdRepository.findByHiveId(hiveId).orElseThrow();
        Hive hive = hiveRepository.findById(hiveId).orElseThrow();

        // Verificar se usuário tem dados suficientes (3+ meses de uso)
        if (!hasEnoughData(hiveId)) {
            throw new BadRequestException("Insufficient data to contribute threshold");
        }

        CommunityThreshold community = CommunityThreshold.builder()
            .name(generateName(hive.getLocation()))
            .description(description)
            .region(getRegion(hive.getLocation()))
            .climate(getClimate(hive.getLocation()))
            .temperatureMin(threshold.getTemperatureMin())
            .temperatureMax(threshold.getTemperatureMax())
            // ... copiar outros valores
            .contributedBy(ownerId)
            .usageCount(1)
            .isVerified(false)
            .build();

        communityThresholdRepository.save(community);
    }

    public void rateThreshold(UUID communityThresholdId, UUID userId, int rating, String review) {
        // Sistema de avaliação por outros usuários
        // Rating 1-5 estrelas + comentários opcionais
    }
}
```

### Analytics e Insights

```java
// Dashboard de analytics para thresholds
@Service
public class ThresholdAnalyticsService {

    public ThresholdInsights generateInsights(UUID hiveId, Period period) {
        Threshold threshold = thresholdRepository.findByHiveId(hiveId).orElseThrow();
        List<Alert> alerts = alertRepository.findByHiveAndPeriod(hiveId, period);
        List<DailyMeasurementAverage> measurements = measurementRepository.findByHiveAndPeriod(hiveId, period);

        return ThresholdInsights.builder()
            .alertFrequency(calculateAlertFrequency(alerts))
            .parameterDistribution(calculateDistribution(measurements))
            .thresholdEffectiveness(calculateEffectiveness(threshold, alerts, measurements))
            .seasonalPatterns(identifySeasonalPatterns(measurements))
            .recommendations(generateRecommendations(threshold, alerts, measurements))
            .comparisonWithSimilarHives(compareWithSimilarHives(hiveId, threshold))
            .build();
    }

    public List<ThresholdAlert> detectProblems(UUID hiveId) {
        List<ThresholdAlert> problems = new ArrayList<>();

        // Detectar problemas como:
        // - Threshold muito permissivo (sem alertas em 30 dias)
        // - Threshold muito restritivo (alertas demais)
        // - Ranges muito estreitos
        // - Valores fora de padrões regionais

        return problems;
    }
}
```

---

Esta documentação cobre todos os aspectos do módulo de Thresholds no TechMel. O sistema é projetado para ser flexível, seguro e escalável, fornecendo a base fundamental para o sistema de monitoramento inteligente de colmeias.
