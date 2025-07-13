package com.tech_mel.tech_mel.infrastructure.security.adapter;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtAdapter implements JwtPort {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    public String generateToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(String token, String tokenType) {
        try {
            Claims claims = extractAllClaims(token);
            String actualTokenType = (String) claims.get("tokenType");

            return tokenType.equals(actualTokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void addToBlacklist(String token) {
        // Removido - será tratado diretamente pelo TokenBlacklistPort
    }

    @Override
    public boolean isBlacklisted(String token) {
        // Removido - será tratado diretamente pelo TokenBlacklistPort
        return false;
    }

    // Métodos auxiliares
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}