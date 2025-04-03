package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.infrastructure.security.oauth2.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final RedisTemplate<String, Object> objectRedisTemplate;

    @GetMapping("/exchange-token")
    public ResponseEntity<Map<String, String>> exchangeToken(@RequestParam String state) {
        String redisKey = "oauth2:state:" + state;
        TokenPair tokenPair = (TokenPair) objectRedisTemplate.opsForValue().get(redisKey);

        if (tokenPair == null) {
            return ResponseEntity.notFound().build();
        }

        // Remover o token temporário após uso
        objectRedisTemplate.delete(redisKey);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", tokenPair.getAccessToken());
        response.put("refreshToken", tokenPair.getRefreshToken());

        return ResponseEntity.ok(response);
    }
}