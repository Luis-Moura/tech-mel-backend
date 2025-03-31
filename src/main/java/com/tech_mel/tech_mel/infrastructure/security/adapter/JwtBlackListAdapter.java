package com.tech_mel.tech_mel.infrastructure.security.adapter;

import com.tech_mel.tech_mel.domain.port.output.JwtBlackListPort;
import com.tech_mel.tech_mel.domain.port.output.JwtOperationsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtBlackListAdapter implements JwtBlackListPort {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist_token:";
    private final JwtOperationsPort jwtOperationsPort;

    @Override
    public void addToBlacklist(String token) {
        long expirationMillis = extractExpirationMillis(token);
        if (expirationMillis > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "revoked", expirationMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    private long extractExpirationMillis(String token) {
        try {
            Date expiration = jwtOperationsPort.extractExpiration(token);

            if (expiration == null) {
                throw new IllegalArgumentException("Token sem data de expiração");
            }

            long timeToExpire = expiration.getTime() - System.currentTimeMillis();


            return Math.max(timeToExpire > 0 ? timeToExpire : 1000, 10 * 60 * 1000);
        } catch (Exception e) {
            return 24 * 60 * 60 * 1000;
        }
    }
}
