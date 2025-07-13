package com.tech_mel.tech_mel.infrastructure.cache.adapter;

import com.tech_mel.tech_mel.domain.port.output.CachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisCacheAdapter implements CachePort {
    
    private final RedisTemplate<String, String> accessTokenRedisTemplate;

    @Override
    public void set(String key, String value, Duration ttl) {
        accessTokenRedisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        String value = accessTokenRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void delete(String key) {
        accessTokenRedisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return accessTokenRedisTemplate.hasKey(key);
    }
}
