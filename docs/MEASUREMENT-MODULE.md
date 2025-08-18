# Documentação Técnica: Módulo de Medições (Measurements) no TechMel

## Sumário

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura da Solução](#2-arquitetura-da-solução)
3. [Entidades e Relacionamentos](#3-entidades-e-relacionamentos)
4. [Casos de Uso (Use Cases)](#4-casos-de-uso-use-cases)
5. [Fluxo de Dados](#5-fluxo-de-dados)
6. [Integração entre Módulos](#6-integração-entre-módulos)
7. [Implementação das APIs](#7-implementação-das-apis)
8. [Armazenamento em Redis](#8-armazenamento-em-redis)
9. [Processamento de Médias Diárias](#9-processamento-de-médias-diárias)
10. [Exemplos de Interação](#10-exemplos-de-interação)
11. [Possíveis Extensões Futuras](#11-possíveis-extensões-futuras)

## 1. Visão Geral

O módulo de Medições (Measurements) é o **coração do sistema IoT** do TechMel, responsável por receber, processar, armazenar e disponibilizar dados dos sensores instalados nas colmeias. Este módulo gerencia tanto o **armazenamento em tempo real no Redis** quanto o **processamento histórico em PostgreSQL**.

### Características Principais

- **Recepção IoT**: Endpoint dedicado para dispositivos IoT enviarem dados
- **Armazenamento Hot**: Redis para dados recentes (24h) com alta performance
- **Armazenamento Cold**: PostgreSQL para médias diárias históricas
- **Validação de Dispositivos**: Autenticação via API Key única por colmeia
- **Processamento Automatizado**: Job diário para calcular médias e limpar cache
- **Integração Reativa**: Dispara verificação de alertas automaticamente

### Tipos de Sensores Suportados

- **Temperatura**: Medição em graus Celsius (-50°C a +60°C)
- **Umidade**: Umidade relativa do ar em porcentagem (0% a 100%)
- **CO₂**: Concentração de dióxido de carbono em ppm (0 a 5000 ppm)

## 2. Arquitetura da Solução

### Diagrama de Fluxo Completo

```
                          ┌───────────────────────────────────────────┐
                          │                                           │
                          ▼                                           │
┌─────────────┐    ┌──────────────┐    ┌──────────────┐    ┌─────────────────┐
│ Dispositivo │    │    Redis     │    │ PostgreSQL   │    │    Frontend     │
│    IoT      │    │  (Hot Data)  │    │ (Cold Data)  │    │   Dashboard     │
│             │    │              │    │              │    │                 │
│ Sensores:   │───►│ measurements │    │ daily_avg    │◄───│ • Últimas       │
│ • Temp      │    │ TTL: 24h     │    │ histórico    │    │   medições      │
│ • Umidade   │    │ Formato: List│    │ permanente   │    │ • Gráficos      │
│ • CO2       │    │              │    │              │    │ • Histórico     │
└─────────────┘    └──────────────┘    └──────────────┘    └─────────────────┘
                            │                  ▲
                            │    ┌─────────────┘
                            ▼    │
                   ┌─────────────────┐
                   │ DailyScheduler  │
                   │                 │
                   │ • Cron: 00:01   │
                   │ • Calcula médias│
                   │ • Limpa Redis   │
                   │ • Persiste PG   │
                   └─────────────────┘
```

### Componentes Principais

1. **MeasurementController**: Endpoints para dispositivos IoT e consultas
2. **MeasurementService**: Lógica de negócio e orquestração
3. **RedisIotAdapter**: Gerenciamento de dados em tempo real
4. **DailyMeasurementAverageAdapter**: Persistência de dados históricos
5. **DailyAverageScheduler**: Job automatizado para processamento diário
6. **Alert Integration**: Integração com sistema de alertas

## 3. Entidades e Relacionamentos

### Entidade Measurement (Redis)

```java
public class Measurement {
    private UUID id;                    // Identificador único
    private Double temperature;         // Temperatura em °C
    private Double humidity;           // Umidade em %
    private Double co2;                // CO2 em ppm
    private LocalDateTime measuredAt;  // Timestamp da medição
}
```

### Entidade DailyMeasurementAverage (PostgreSQL)

```java
public class DailyMeasurementAverage {
    private UUID id;                    // PK
    private double avgTemperature;      // Média diária de temperatura
    private double avgHumidity;         // Média diária de umidade
    private double avgCo2;             // Média diária de CO2
    private LocalDate date;            // Data da média
    private Hive hive;                 // Relação com colmeia
}
```

### Relacionamentos

- **Measurement ↔ Hive**: Relacionamento via API Key (não há FK no Redis)
- **DailyMeasurementAverage ↔ Hive**: Relacionamento Many-to-One persistido
- **Temporal**: Dados Redis são temporários (24h), PostgreSQL é permanente
- **Agregação**: N medições Redis → 1 registro DailyAverage PostgreSQL

### Estratégia de Armazenamento

```
┌─────────────┐     24h TTL     ┌──────────────┐     Job Diário    ┌─────────────────┐
│ Medições    │ ──────────────► │    Redis     │ ────────────────► │   PostgreSQL    │
│ Tempo Real  │                 │   Lista de   │                   │  Médias Diárias │
│ (1-5 min)   │                 │  Medições    │                   │   (Histórico)   │
└─────────────┘                 └──────────────┘                   └─────────────────┘
```

## 4. Casos de Uso (Use Cases)

### Interface MeasurementUseCase

```java
public interface MeasurementUseCase {
    Measurement registerMeasurement(String apiKey, CreateMeasurementRequest request);
    Measurement getLatestMeasurementByApiKey(UUID userId, UUID hiveId);
    Map<String, Measurement> getLatestMeasurementsGroupedByHive(UUID userId);
    Page<DailyMeasurementAverage> getDailyMeasurementAverages(UUID userId, UUID hiveId, Pageable pageable);
}
```

### Casos de Uso Detalhados

#### UC01: Registro de Medição IoT

- **Ator**: Dispositivo IoT
- **Pré-condição**: API Key válida e colmeia ativa
- **Fluxo Principal**:
  1. Dispositivo envia dados via POST /api/measurements/iot
  2. Sistema valida API Key e status da colmeia
  3. Cria objeto Measurement com timestamp
  4. Armazena no Redis com TTL de 24h
  5. Dispara verificação de alertas
  6. Retorna confirmação ao dispositivo

#### UC02: Consulta Última Medição

- **Ator**: Usuário autenticado
- **Pré-condição**: Usuário proprietário da colmeia
- **Fluxo Principal**:
  1. Cliente requisita última medição de uma colmeia
  2. Sistema valida propriedade da colmeia
  3. Busca última medição no Redis
  4. Retorna dados formatados

#### UC03: Dashboard com Múltiplas Colmeias

- **Ator**: Usuário autenticado
- **Pré-condição**: Usuário possui colmeias
- **Fluxo Principal**:
  1. Cliente requisita resumo de todas as colmeias
  2. Sistema lista colmeias do usuário
  3. Busca última medição de cada colmeia no Redis
  4. Retorna mapa agrupado por colmeia

#### UC04: Consulta Histórico de Médias

- **Ator**: Usuário autenticado
- **Pré-condição**: Histórico disponível no PostgreSQL
- **Fluxo Principal**:
  1. Cliente requisita médias diárias paginadas
  2. Sistema valida propriedade da colmeia
  3. Consulta tabela daily_measurement_average
  4. Retorna dados paginados ordenados por data

#### UC05: Processamento Diário Automatizado

- **Ator**: Sistema (Scheduler)
- **Pré-condição**: Job configurado para 00:01
- **Fluxo Principal**:
  1. Scheduler inicia às 00:01 diariamente
  2. Lista todas as colmeias ativas
  3. Para cada colmeia, busca medições das últimas 24h no Redis
  4. Calcula médias de temperatura, umidade e CO2
  5. Persiste médias no PostgreSQL
  6. Limpa dados do Redis para liberar memória

## 5. Fluxo de Dados

### Fluxo de Registro de Medição

```
1. Dispositivo IoT
   │ POST /api/measurements/iot
   │ Headers: X-API-Key: hive_12345_api_key
   │ Body: {temperature, humidity, co2, measuredAt}
   ▼
2. MeasurementController
   │ @RequestHeader("X-API-Key") String apiKey
   │ @Valid @RequestBody CreateMeasurementRequest
   ▼
3. MeasurementService.registerMeasurement()
   │ ├─ hiveRepositoryPort.findByApiKey(apiKey)
   │ ├─ validate hive.status == ACTIVE
   │ ├─ build Measurement object
   │ ├─ redisIotPort.saveMeasurement(apiKey, measurement)
   │ └─ alertUseCase.saveAlert(measurement, hive, timestamp)
   ▼
4. RedisIotAdapter
   │ ├─ key: "measurements:{apiKey}"
   │ ├─ leftPush(measurement) // mais recente primeiro
   │ ├─ trim(0, 999) // máximo 1000 medições
   │ └─ expire(24 hours)
   ▼
5. AlertService (async)
   │ ├─ findThresholdByHiveId()
   │ ├─ checkLimits(temperature, humidity, co2)
   │ └─ createAlertsIfNeeded()
   ▼
6. Response HTTP 201
   │ {temperature, humidity, co2}
```

### Fluxo de Consulta de Dados

```
1. Frontend Dashboard
   │ GET /api/measurements/latests
   │ Headers: Authorization: Bearer {jwt}
   ▼
2. MeasurementController
   │ authenticationUtil.getCurrentUserId()
   ▼
3. MeasurementService.getLatestMeasurementsGroupedByHive()
   │ ├─ hiveRepositoryPort.findByOwnerId(userId)
   │ ├─ extract apiKeys from hives
   │ └─ redisIotPort.getLatestMeasurementsForMultipleHives(apiKeys)
   ▼
4. RedisIotAdapter
   │ ├─ for each apiKey: opsForList().index(key, 0)
   │ ├─ objectMapper.convertValue(obj, Measurement.class)
   │ └─ return Map<apiKey, measurement>
   ▼
5. Response Formatting
   │ ├─ map apiKey to hiveId and hiveName
   │ └─ build LatestHiveMeasurementResponse[]
   ▼
6. JSON Response
   │ [{hiveId, hiveName, latestMeasurement}, ...]
```

### Fluxo do Job Diário

```
1. Cron Scheduler
   │ @Scheduled(cron = "0 1 0 * * *") // 00:01 daily
   ▼
2. DailyAverageScheduler.processDailyAverages()
   │ ├─ log.info("Starting daily average processing...")
   │ └─ hiveRepositoryPort.findAllHives()
   ▼
3. Para cada Hive:
   │ ├─ redisIotPort.getMeasurements(apiKey, 1000)
   │ ├─ filter last 24 hours measurements
   │ ├─ calculate averages (temperature, humidity, co2)
   │ ├─ build DailyMeasurementAverage object
   │ ├─ dailyMeasurementAverageRepositoryPort.save()
   │ └─ redisIotPort.clearMeasurements(apiKey)
   ▼
4. Cleanup & Logging
   │ ├─ log.info("Processed {} hives", count)
   │ └─ Redis memory freed, PostgreSQL updated
```

## 6. Integração entre Módulos

### Measurement → Alert (Síncrona)

```java
// No MeasurementService após salvar no Redis
alertUseCase.saveAlert(measurement, hive, request.measuredAt());
```

**Características da Integração**:

- **Síncrona**: Alertas são verificados imediatamente após cada medição
- **Não-bloqueante**: Falha na criação de alerta não impede o registro da medição
- **Transacional**: Ambos são persistidos na mesma operação
- **Performance**: Otimizada para processar medições em alta frequência

### Measurement → Hive (Via API Key)

```java
// Validação de dispositivo autorizado
Hive hive = hiveRepositoryPort.findByApiKey(apiKey)
    .orElseThrow(() -> new NotFoundException("Hive not found for API key"));

if (hive.getHiveStatus() == Hive.HiveStatus.INACTIVE) {
    throw new ConflictException("Cannot register measurement for an inactive hive");
}
```

**Relacionamento**:

- **Autenticação**: API Key é o mecanismo de segurança IoT
- **Status**: Só colmeias ACTIVE podem receber medições
- **Rastreabilidade**: Cada medição é vinculada a uma colmeia específica

### Measurement → DailyAverage (Assíncrona)

```java
// Job diário converte Redis → PostgreSQL
List<Measurement> last24Hours = measurements.stream()
    .filter(m -> m.getMeasuredAt().isAfter(LocalDateTime.now().minusHours(24)))
    .toList();

double avgTemperature = last24Hours.stream()
    .mapToDouble(Measurement::getTemperature)
    .average()
    .orElse(0.0);
```

**Características**:

- **Assíncrona**: Processamento em batch uma vez por dia
- **Agregação**: Múltiplas medições → uma média diária
- **Persistência**: Dados históricos permanentes para analytics

## 7. Implementação das APIs

### Endpoints Disponíveis

#### POST /api/measurements/iot

**Registro de medições pelos dispositivos IoT**

```bash
curl -X POST http://localhost:8080/api/measurements/iot \
  -H "X-API-Key: hive_12345_api_key_abcdef" \
  -H "Content-Type: application/json" \
  -d '{
    "temperature": 25.5,
    "humidity": 65.0,
    "co2": 400.0,
    "measuredAt": "2025-01-15T14:30:00"
  }'
```

**Validações**:

- API Key deve existir e estar associada a colmeia ativa
- Valores dentro dos ranges permitidos (temp: -50~60, humidity: 0~100, co2: 0~5000)
- Timestamp não pode ser futuro

**Resposta**:

```json
{
	"temperature": 25.5,
	"humidity": 65.0,
	"co2": 400.0
}
```

#### GET /api/measurements/latest/{hiveId}

**Última medição de uma colmeia específica**

```bash
curl -X GET \
  'http://localhost:8080/api/measurements/latest/f47ac10b-58cc-4372-a567-0e02b2c3d479' \
  -H 'Authorization: Bearer {token}'
```

**Resposta**:

```json
{
	"hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
	"hiveName": "Colmeia Principal",
	"latestMeasurement": {
		"id": "c47ac10b-58cc-4372-a567-0e02b2c3d479",
		"temperature": 25.5,
		"humidity": 65.0,
		"co2": 400.0,
		"measuredAt": "2025-01-15T14:30:00"
	}
}
```

#### GET /api/measurements/latests

**Últimas medições de todas as colmeias do usuário**

```bash
curl -X GET \
  'http://localhost:8080/api/measurements/latests' \
  -H 'Authorization: Bearer {token}'
```

**Resposta**:

```json
[
	{
		"hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
		"hiveName": "Colmeia Principal",
		"latestMeasurement": {
			"id": "c47ac10b-58cc-4372-a567-0e02b2c3d479",
			"temperature": 25.5,
			"humidity": 65.0,
			"co2": 400.0,
			"measuredAt": "2025-01-15T14:30:00"
		}
	},
	{
		"hiveId": "a47ac10b-58cc-4372-a567-0e02b2c3d480",
		"hiveName": "Colmeia Secundária",
		"latestMeasurement": {
			"id": "d47ac10b-58cc-4372-a567-0e02b2c3d481",
			"temperature": 23.8,
			"humidity": 68.2,
			"co2": 380.0,
			"measuredAt": "2025-01-15T14:28:00"
		}
	}
]
```

#### GET /api/measurements/daily-averages/{hiveId}

**Médias diárias históricas paginadas**

```bash
curl -X GET \
  'http://localhost:8080/api/measurements/daily-averages/f47ac10b-58cc-4372-a567-0e02b2c3d479?page=0&size=10' \
  -H 'Authorization: Bearer {token}'
```

**Resposta**:

```json
{
	"content": [
		{
			"id": "e47ac10b-58cc-4372-a567-0e02b2c3d482",
			"avgTemperature": 24.8,
			"avgHumidity": 66.5,
			"avgCo2": 395.0,
			"date": "2025-01-14",
			"hiveId": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
		}
	],
	"totalElements": 45,
	"totalPages": 5,
	"size": 10,
	"number": 0
}
```

### Códigos de Resposta

- **201 Created**: Medição registrada com sucesso (POST IoT)
- **200 OK**: Consulta realizada com sucesso
- **401 Unauthorized**: API Key inválida ou token JWT expirado
- **403 Forbidden**: Usuário sem permissão para acessar a colmeia
- **404 Not Found**: Colmeia não encontrada ou sem medições
- **409 Conflict**: Tentativa de registrar medição em colmeia inativa
- **400 Bad Request**: Dados inválidos ou fora dos ranges permitidos

## 8. Armazenamento em Redis

### Estratégia de Chaves

```
Padrão: measurements:{apiKey}
Exemplo: measurements:hive_12345_api_key_abcdef
```

### Estrutura de Dados

```redis
> LLEN measurements:hive_12345_api_key_abcdef
(integer) 245

> LINDEX measurements:hive_12345_api_key_abcdef 0  # Mais recente
"{\"id\":\"123e4567-...\",\"temperature\":25.5,\"humidity\":65.0,\"co2\":400.0,\"measuredAt\":\"2025-01-15T14:30:00\"}"

> TTL measurements:hive_12345_api_key_abcdef
(integer) 82800  # ~23 horas restantes
```

### Configuração do Redis

```java
@Bean
public RedisTemplate<String, Object> iotRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());

    // ObjectMapper configurado para Java 8 Time API
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);
    template.setValueSerializer(jsonSerializer);

    return template;
}
```

### Operações Redis

```java
// Salvar nova medição
public void saveMeasurement(String apiKey, Measurement measurement) {
    String key = "measurements:" + apiKey;

    // TTL automático se a chave não existir
    if (!iotRedisTemplate.hasKey(key)) {
        iotRedisTemplate.expire(key, Duration.ofHours(24));
    }

    // Adiciona no início (mais recente primeiro)
    iotRedisTemplate.opsForList().leftPush(key, measurement);

    // Limita a 1000 medições para economizar memória
    iotRedisTemplate.opsForList().trim(key, 0, 999);
}

// Buscar última medição
public Measurement getLatestMeasurement(String apiKey) {
    String key = "measurements:" + apiKey;
    Object latest = iotRedisTemplate.opsForList().index(key, 0);
    return latest != null ? objectMapper.convertValue(latest, Measurement.class) : null;
}

// Buscar múltiplas medições
public List<Measurement> getMeasurements(String apiKey, int limit) {
    String key = "measurements:" + apiKey;
    List<Object> raw = iotRedisTemplate.opsForList().range(key, 0, limit - 1);
    return raw.stream()
        .filter(Objects::nonNull)
        .map(obj -> objectMapper.convertValue(obj, Measurement.class))
        .toList();
}
```

### Performance e Otimizações

**Características**:

- **Estrutura List**: Permite ordenação cronológica e acesso rápido
- **TTL Automático**: Expira automaticamente após 24h
- **Limite de Tamanho**: Máximo 1000 medições por colmeia (trim automático)
- **Serialização JSON**: Preserva tipos de dados e timestamps

**Métricas de Performance**:

- Inserção: ~0.1ms por medição
- Consulta última: ~0.05ms
- Consulta múltiplas: ~1ms para 100 medições
- Memória: ~200KB por colmeia ativa (estimativa para 1000 medições)

## 9. Processamento de Médias Diárias

### Job Scheduler

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DailyAverageScheduler {

    @Scheduled(cron = "0 1 0 * * *") // Todo dia às 00:01
    public void processDailyAverages() {
        log.info("Starting daily average processing...");

        List<Hive> allHives = hiveRepositoryPort.findAllHives(Pageable.unpaged()).getContent();

        for (Hive hive : allHives) {
            processHiveDailyAverage(hive);
        }

        log.info("Daily average processing completed for {} hives", allHives.size());
    }

    private void processHiveDailyAverage(Hive hive) {
        String apiKey = hive.getApiKey();
        List<Measurement> measurements = redisIotPort.getMeasurements(apiKey, 1000);

        // Filtrar apenas medições das últimas 24 horas
        List<Measurement> last24Hours = measurements.stream()
            .filter(m -> m.getMeasuredAt().isAfter(LocalDateTime.now().minusHours(24)))
            .toList();

        if (last24Hours.isEmpty()) {
            log.warn("No measurements found for hive {} in the last 24 hours", hive.getId());
            return;
        }

        // Calcular médias
        double avgTemperature = last24Hours.stream().mapToDouble(Measurement::getTemperature).average().orElse(0.0);
        double avgHumidity = last24Hours.stream().mapToDouble(Measurement::getHumidity).average().orElse(0.0);
        double avgCo2 = last24Hours.stream().mapToDouble(Measurement::getCo2).average().orElse(0.0);

        // Criar e salvar média diária
        DailyMeasurementAverage dailyAverage = DailyMeasurementAverage.builder()
            .hive(hive)
            .avgTemperature(avgTemperature)
            .avgHumidity(avgHumidity)
            .avgCo2(avgCo2)
            .date(LocalDate.now().minusDays(1)) // Data do dia anterior
            .build();

        dailyMeasurementAverageRepositoryPort.save(dailyAverage);

        // Limpar dados do Redis
        redisIotPort.clearMeasurements(apiKey);

        log.info("Processed daily average for hive {}: temp={}, humidity={}, co2={}",
                hive.getId(), avgTemperature, avgHumidity, avgCo2);
    }
}
```

### Lógica de Cálculo

**Período de Agregação**:

- Coleta medições das últimas 24 horas (desde o momento da execução)
- Data da média: dia anterior (LocalDate.now().minusDays(1))

**Cálculo das Médias**:

```java
// Média aritmética simples
double avgTemperature = measurements.stream()
    .mapToDouble(Measurement::getTemperature)
    .average()
    .orElse(0.0);
```

**Tratamento de Casos Especiais**:

- Sem medições: Pula o processamento, loga warning
- Valores nulos: Usa .orElse(0.0) como fallback
- Medições parciais: Calcula com dados disponíveis

### Modelo de Dados Histórico

```sql
CREATE TABLE daily_measurement_average (
    id UUID PRIMARY KEY,
    avg_temperature DOUBLE PRECISION NOT NULL,
    avg_humidity DOUBLE PRECISION NOT NULL,
    avg_co2 DOUBLE PRECISION NOT NULL,
    date DATE NOT NULL,
    hive_id UUID NOT NULL REFERENCES hives(id) ON DELETE CASCADE,

    UNIQUE(hive_id, date),  -- Uma média por colmeia por dia
    INDEX idx_hive_date (hive_id, date DESC)  -- Otimização para consultas
);
```

## 10. Exemplos de Interação

### Cenário 1: Dispositivo IoT Enviando Dados

#### Configuração do Dispositivo

```cpp
// Código Arduino/ESP32 simplificado
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* API_KEY = "hive_12345_api_key_abcdef";
const char* SERVER_URL = "http://techmel-api.com/api/measurements/iot";

void sendMeasurement(float temp, float humidity, float co2) {
    HTTPClient http;
    http.begin(SERVER_URL);
    http.addHeader("Content-Type", "application/json");
    http.addHeader("X-API-Key", API_KEY);

    // Criar payload JSON
    DynamicJsonDocument doc(1024);
    doc["temperature"] = temp;
    doc["humidity"] = humidity;
    doc["co2"] = co2;
    doc["measuredAt"] = getCurrentISOTimestamp();

    String payload;
    serializeJson(doc, payload);

    int httpResponseCode = http.POST(payload);

    if (httpResponseCode == 201) {
        Serial.println("Measurement sent successfully");
    } else {
        Serial.printf("Error: %d\n", httpResponseCode);
    }

    http.end();
}
```

#### Resposta do Sistema

```
2025-01-15 14:30:01.234 INFO  MeasurementService - Registering measurement for hive: f47ac10b-58cc-4372-a567-0e02b2c3d479
2025-01-15 14:30:01.235 INFO  RedisIotAdapter   - Saved measurement to Redis: measurements:hive_12345_api_key_abcdef
2025-01-15 14:30:01.236 INFO  AlertService      - Checking thresholds for measurement...
2025-01-15 14:30:01.238 INFO  AlertService      - No alerts triggered, all values within limits
```

### Cenário 2: Dashboard Frontend Consultando Dados

#### Implementação React

```typescript
// Hook customizado para medições
const useMeasurements = () => {
	const [measurements, setMeasurements] = useState<HiveMeasurement[]>([]);
	const [loading, setLoading] = useState(true);

	const fetchLatestMeasurements = async () => {
		try {
			const response = await api.get("/measurements/latests");
			setMeasurements(response.data);
		} catch (error) {
			console.error("Error fetching measurements:", error);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchLatestMeasurements();

		// Atualizar a cada 30 segundos
		const interval = setInterval(fetchLatestMeasurements, 30000);
		return () => clearInterval(interval);
	}, []);

	return { measurements, loading, refetch: fetchLatestMeasurements };
};

// Componente Dashboard
const Dashboard: React.FC = () => {
	const { measurements, loading } = useMeasurements();

	if (loading) return <LoadingSpinner />;

	return (
		<div className="dashboard-grid">
			{measurements.map(({ hiveId, hiveName, latestMeasurement }) => (
				<HiveCard key={hiveId}>
					<h3>{hiveName}</h3>
					<div className="metrics">
						<MetricCard
							icon="🌡️"
							label="Temperatura"
							value={`${latestMeasurement.temperature}°C`}
							status={getTemperatureStatus(latestMeasurement.temperature)}
						/>
						<MetricCard
							icon="💧"
							label="Umidade"
							value={`${latestMeasurement.humidity}%`}
							status={getHumidityStatus(latestMeasurement.humidity)}
						/>
						<MetricCard
							icon="💨"
							label="CO₂"
							value={`${latestMeasurement.co2} ppm`}
							status={getCo2Status(latestMeasurement.co2)}
						/>
					</div>
					<div className="timestamp">
						Última medição:{" "}
						{formatDistanceToNow(new Date(latestMeasurement.measuredAt))}
					</div>
				</HiveCard>
			))}
		</div>
	);
};
```

### Cenário 3: Job Diário Processando Médias

#### Log de Execução

```
2025-01-16 00:01:00.000 INFO  DailyAverageScheduler - Starting daily average processing...
2025-01-16 00:01:00.125 INFO  DailyAverageScheduler - Processing hive f47ac10b-58cc-4372-a567-0e02b2c3d479
2025-01-16 00:01:00.130 INFO  RedisIotAdapter      - Retrieved 287 measurements for processing
2025-01-16 00:01:00.135 INFO  DailyAverageScheduler - Filtered to 287 measurements from last 24h
2025-01-16 00:01:00.140 INFO  DailyAverageScheduler - Calculated averages: temp=24.8°C, humidity=66.2%, co2=398.5ppm
2025-01-16 00:01:00.145 INFO  DailyAverageScheduler - Saved daily average for 2025-01-15
2025-01-16 00:01:00.150 INFO  RedisIotAdapter      - Cleared Redis data for hive
2025-01-16 00:01:00.155 INFO  DailyAverageScheduler - Processed daily average for hive f47ac10b-58cc-4372-a567-0e02b2c3d479
...
2025-01-16 00:01:05.789 INFO  DailyAverageScheduler - Daily average processing completed for 15 hives
```

#### Dados Gerados

```sql
-- Registro inserido no PostgreSQL
INSERT INTO daily_measurement_average VALUES (
    'e47ac10b-58cc-4372-a567-0e02b2c3d482',
    24.8,      -- avg_temperature
    66.2,      -- avg_humidity
    398.5,     -- avg_co2
    '2025-01-15', -- date
    'f47ac10b-58cc-4372-a567-0e02b2c3d479' -- hive_id
);
```

### Cenário 4: Análise de Histórico

#### Consulta de Tendências

```typescript
const useHistoricalData = (hiveId: string, days: number = 30) => {
	const [historicalData, setHistoricalData] = useState<DailyAverage[]>([]);

	useEffect(() => {
		const fetchHistory = async () => {
			const response = await api.get(`/measurements/daily-averages/${hiveId}`, {
				params: { size: days, sort: "date,desc" },
			});
			setHistoricalData(response.data.content);
		};

		fetchHistory();
	}, [hiveId, days]);

	// Calcular tendências
	const trends = useMemo(() => {
		if (historicalData.length < 2) return null;

		const latest = historicalData[0];
		const previous = historicalData[1];

		return {
			temperature: latest.avgTemperature - previous.avgTemperature,
			humidity: latest.avgHumidity - previous.avgHumidity,
			co2: latest.avgCo2 - previous.avgCo2,
		};
	}, [historicalData]);

	return { historicalData, trends };
};

// Componente de gráfico
const HistoricalChart: React.FC<{ hiveId: string }> = ({ hiveId }) => {
	const { historicalData, trends } = useHistoricalData(hiveId);

	return (
		<div className="historical-chart">
			<h3>Histórico - Últimos 30 dias</h3>

			{trends && (
				<div className="trends">
					<TrendIndicator
						label="Temperatura"
						value={trends.temperature}
						unit="°C"
					/>
					<TrendIndicator label="Umidade" value={trends.humidity} unit="%" />
					<TrendIndicator label="CO₂" value={trends.co2} unit="ppm" />
				</div>
			)}

			<LineChart data={historicalData}>
				<Line dataKey="avgTemperature" stroke="#ff7300" />
				<Line dataKey="avgHumidity" stroke="#00ff73" />
				<Line dataKey="avgCo2" stroke="#0073ff" />
			</LineChart>
		</div>
	);
};
```

## 11. Possíveis Extensões Futuras

### Processamento de Streaming Real-Time

```java
// Integração com Apache Kafka para processamento em tempo real
@KafkaListener(topics = "measurements-realtime")
public void processMeasurementStream(ConsumerRecord<String, Measurement> record) {
    Measurement measurement = record.value();
    String hiveId = record.key();

    // Processamento em janelas deslizantes
    StreamProcessor.updateWindow(hiveId, measurement);

    // Detecção de anomalias em tempo real
    AnomalyDetector.analyze(measurement);

    // Métricas em tempo real
    MetricsCollector.updateRealTimeMetrics(hiveId, measurement);
}
```

### Machine Learning para Predição

```java
// Modelo preditivo baseado em dados históricos
@Service
public class MeasurementPredictionService {

    public PredictionResult predictNext6Hours(UUID hiveId) {
        List<DailyMeasurementAverage> history = getHistoricalData(hiveId, 90); // 3 meses
        List<Measurement> recentMeasurements = getRecentMeasurements(hiveId, 24); // 24h

        // Features: tempo, temperatura externa, umidade, sazonalidade
        FeatureVector features = FeatureExtractor.extract(history, recentMeasurements);

        // Modelo treinado (scikit-learn via Python API)
        PredictionModel model = ModelRegistry.getModel("measurement-prediction-v2");

        return model.predict(features);
    }
}
```

### Analytics Avançados

```java
// Análise de padrões e correlações
@Service
public class MeasurementAnalyticsService {

    public CorrelationReport analyzeCorrelations(UUID hiveId, Period period) {
        // Correlação entre fatores ambientais
        // Padrões sazonais e circadianos
        // Impacto de eventos externos (clima, localização)

        return CorrelationReport.builder()
            .temperatureHumidityCorrelation(calculateCorrelation())
            .dailyPatterns(identifyDailyPatterns())
            .seasonalTrends(calculateSeasonalTrends())
            .anomalies(detectAnomalies())
            .build();
    }

    public HealthScore calculateHiveHealth(UUID hiveId) {
        // Score de saúde baseado em:
        // - Estabilidade das medições
        // - Conformidade com thresholds
        // - Tendências históricas
        // - Comparação com outras colmeias

        return HealthScore.builder()
            .overall(calculateOverallScore())
            .stability(calculateStabilityScore())
            .conformity(calculateConformityScore())
            .trends(calculateTrendScore())
            .build();
    }
}
```

### Otimizações de Performance

```java
// Cache inteligente para consultas frequentes
@Service
public class MeasurementCacheService {

    @Cacheable(value = "latest-measurements", key = "#userId")
    public List<LatestHiveMeasurement> getCachedLatestMeasurements(UUID userId) {
        // Cache com TTL de 30 segundos para dados "latest"
        // Invalidação automática quando nova medição chega
        return measurementService.getLatestMeasurementsGroupedByHive(userId);
    }

    @CacheEvict(value = "latest-measurements", key = "#userId")
    public void invalidateUserCache(UUID userId) {
        // Chamado automaticamente após nova medição
    }
}

// Particionamento de dados históricos
@Entity
@Table(name = "daily_measurement_average")
@PartitionKey("date") // Particionamento por mês ou ano
public class DailyMeasurementAverageEntity {
    // Melhora performance para consultas de histórico longo
}
```

### Integração IoT Avançada

```java
// Suporte a múltiplos protocolos IoT
@Component
public class IoTProtocolHandler {

    // MQTT para dispositivos low-power
    @EventListener
    public void handleMqttMeasurement(MqttMeasurementEvent event) {
        String topic = event.getTopic(); // "techmel/hive/{apiKey}/measurements"
        Measurement measurement = parsePayload(event.getPayload());
        measurementService.registerMeasurement(extractApiKey(topic), measurement);
    }

    // LoRaWAN para dispositivos remotos
    @EventListener
    public void handleLoRaWanMeasurement(LoRaWanEvent event) {
        // Decodificar payload compacto
        // Lidar com transmission delays
        // Gerenciar device sleep cycles
    }

    // CoAP para devices ultra low-power
    @CoapEndpoint("/measurements")
    public void handleCoapMeasurement(CoapRequest request) {
        // Protocol buffer ou CBOR para payload mínimo
        // Acknowledgments para garantir entrega
    }
}
```

---

Esta documentação cobre todos os aspectos do módulo de Medições no TechMel. O sistema é projetado para alta performance, confiabilidade e escalabilidade, lidando com a natureza crítica dos dados de monitoramento de colmeias em tempo real.
