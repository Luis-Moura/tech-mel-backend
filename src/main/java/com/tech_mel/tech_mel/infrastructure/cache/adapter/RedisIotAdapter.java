package com.tech_mel.tech_mel.infrastructure.cache.adapter;

import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.output.RedisIotPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisIotAdapter implements RedisIotPort {
    private final RedisTemplate<String, Object> iotRedisTemplate;
    
    private static final String MEASUREMENT_KEY_PREFIX = "measurements:";
    private static final long DEFAULT_TTL_HOURS = 24; // TTL de 24 horas para as medições

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
        
        // Busca as medições mais recentes (limitadas pelo parâmetro)
        List<Object> measurements = iotRedisTemplate.opsForList().range(key, 0, limit - 1);
        
        if (measurements == null) {
            return List.of();
        }
        
        return measurements.stream()
                .filter(Objects::nonNull)
                .map(obj -> (Measurement) obj)
                .toList();
    }

    @Override
    public Measurement getLatestMeasurement(String apiKey) {
        String key = MEASUREMENT_KEY_PREFIX + apiKey;
        
        // Busca a primeira medição da lista (mais recente)
        Object latestMeasurement = iotRedisTemplate.opsForList().index(key, 0);
        
        return latestMeasurement != null ? (Measurement) latestMeasurement : null;
    }

    @Override
    public void clearMeasurements(String apiKey) {
        String key = MEASUREMENT_KEY_PREFIX + apiKey;
        iotRedisTemplate.delete(key);
    }
}
