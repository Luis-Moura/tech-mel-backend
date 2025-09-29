package com.tech_mel.tech_mel.infrastructure.cache.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.output.RedisIotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisIotAdapter implements RedisIotPort {
    private final RedisTemplate<String, Object> iotRedisTemplate;

    private static final String MEASUREMENT_KEY_PREFIX = "measurements:";
    private static final long DEFAULT_TTL_HOURS = 24; // TTL de 24 horas para as medições

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void saveMeasurement(String apiKey, Measurement measurement) {
        String key = MEASUREMENT_KEY_PREFIX + apiKey;

        // Define TTL para a chave se ela não existir
        if (!iotRedisTemplate.hasKey(key)) {
            iotRedisTemplate.expire(key, java.time.Duration.ofHours(DEFAULT_TTL_HOURS));
        }

        // Adiciona a medição no início da lista (mais recente primeiro)
        iotRedisTemplate.opsForList().leftPush(key, measurement);

        // Mantém apenas as últimas 1000 medições para evitar crescimento excessivo
        iotRedisTemplate.opsForList().trim(key, 0, 999);
    }

    @Override
    public List<Measurement> getMeasurements(String apiKey, int limit) {
        String key = MEASUREMENT_KEY_PREFIX + apiKey;

        List<Object> rawMeasurements = iotRedisTemplate.opsForList().range(key, 0, limit - 1);

        if (rawMeasurements == null) {
            return List.of();
        }

        return rawMeasurements.stream()
                .filter(Objects::nonNull)
                .map(obj -> objectMapper.convertValue(obj, Measurement.class))
                .toList();
    }

    @Override
    public Map<String, Measurement> getLatestMeasurementsForMultipleHives(List<String> apiKeys) {
        // Evita NPE ao lidar com lista nula ou vazia
        if (apiKeys == null || apiKeys.isEmpty()) {
            return Map.of();
        }

        // Filtra apiKeys nulas antes e só cria o entry quando a measurement não é nula
        return apiKeys.stream()
                .filter(Objects::nonNull)
                .map(apiKey -> {
                    Measurement latest = getLatestMeasurement(apiKey);
                    return latest == null ? null : Map.entry(apiKey, latest);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Measurement getLatestMeasurement(String apiKey) {
        String key = MEASUREMENT_KEY_PREFIX + apiKey;

        Object latestMeasurement = iotRedisTemplate.opsForList().index(key, 0);

        return latestMeasurement != null
                ? objectMapper.convertValue(latestMeasurement, Measurement.class)
                : null;
    }

    @Override
    public void clearMeasurements(String apiKey) {
        String key = MEASUREMENT_KEY_PREFIX + apiKey;
        iotRedisTemplate.delete(key);
    }
}
