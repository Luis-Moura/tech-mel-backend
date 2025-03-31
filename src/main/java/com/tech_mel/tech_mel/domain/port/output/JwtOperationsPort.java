package com.tech_mel.tech_mel.domain.port.output;

import io.jsonwebtoken.Claims;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public interface JwtOperationsPort {
    String generateToken(Map<String, Object> claims, String subject, long expiration);

    String extractUsername(String token);

    Date extractExpiration(String token);

    String extractTokenType(String token);

    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    boolean isTokenValid(String token, String expectedTokenType);
}