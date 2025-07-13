package com.tech_mel.tech_mel.infrastructure.cache.adapter;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.tech_mel.tech_mel.domain.port.output.OAuth2StatePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisOAuth2StateAdapter implements OAuth2StatePort {
    
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Override
    public void storeTokenPair(UUID stateId, Object tokenPair, Duration ttl) {
        String key = "oauth2:state:" + stateId.toString();
        objectRedisTemplate.opsForValue().set(key, tokenPair, ttl);
    }

    @Override
    public Optional<Object> getTokenPair(UUID stateId) {
        String key = "oauth2:state:" + stateId.toString();
        Object tokenPair = objectRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(tokenPair);
    }

    @Override
    public void deleteState(UUID stateId) {
        String key = "oauth2:state:" + stateId.toString();
        objectRedisTemplate.delete(key);
    }
}
