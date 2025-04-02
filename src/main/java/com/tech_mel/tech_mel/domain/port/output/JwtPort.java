package com.tech_mel.tech_mel.domain.port.output;

import io.jsonwebtoken.Claims;

import java.util.Map;

public interface JwtPort {
    String generateToken(Map<String, Object> claims, String subject, long expiration);

    String extractUsername(String token);

    boolean isTokenValid(String token, String tokenType);

    void addToBlacklist(String token);

    boolean isBlacklisted(String token);

    Claims extractAllClaims(String token);
}
