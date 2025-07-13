package com.tech_mel.tech_mel.infrastructure.cache.adapter;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.tech_mel.tech_mel.domain.port.output.PasswordResetCachePort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisPasswordResetAdapter implements PasswordResetCachePort {
    
    private final RedisTemplate<String, String> accessTokenRedisTemplate;

    @Override
    public void storeResetToken(UUID token, UUID userId, Duration ttl) {
        String key = "password-reset:" + token.toString();
        accessTokenRedisTemplate.opsForValue().set(key, userId.toString(), ttl);
    }

    @Override
    public Optional<UUID> getUserIdByToken(UUID token) {
        String key = "password-reset:" + token.toString();
        String userIdStr = accessTokenRedisTemplate.opsForValue().get(key);
        
        if (userIdStr != null) {
            try {
                return Optional.of(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteResetToken(UUID token) {
        String key = "password-reset:" + token.toString();
        accessTokenRedisTemplate.delete(key);
    }
}
