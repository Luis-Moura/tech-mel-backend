package com.tech_mel.tech_mel.infrastructure.cache.adapter;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.tech_mel.tech_mel.domain.port.output.TokenBlacklistPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisTokenBlacklistAdapter implements TokenBlacklistPort {
    
    private final RedisTemplate<String, String> accessTokenRedisTemplate;
    
    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public void addToBlacklist(String token) {
        try {
            // Extrair expiração diretamente no adapter
            Date expiration = extractExpiration(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                accessTokenRedisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Se não conseguir extrair expiração, usar TTL padrão de 24 horas
            accessTokenRedisTemplate.opsForValue().set("blacklist:" + token, "1", 24, TimeUnit.HOURS);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return accessTokenRedisTemplate.hasKey("blacklist:" + token);
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
