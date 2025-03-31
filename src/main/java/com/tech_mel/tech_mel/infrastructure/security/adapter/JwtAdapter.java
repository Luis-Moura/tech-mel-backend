package com.tech_mel.tech_mel.infrastructure.security.adapter;

import com.tech_mel.tech_mel.domain.port.output.JwtOperationsPort;
import com.tech_mel.tech_mel.domain.port.output.JwtBlackListPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtAdapter implements JwtOperationsPort {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.algorithm:HS512}")
    private String algorithm;

    private Key secretKey;

    private SignatureAlgorithm signatureAlgorithm;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.signatureAlgorithm = SignatureAlgorithm.forName(algorithm);
    }

    @Override
    public String generateToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .setIssuer("tech-mel-api")
                .setAudience("tech-mel-client")
                .signWith(this.secretKey, signatureAlgorithm)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public boolean isTokenValid(String token, String expectedTokenType) {
        try {
            Claims claims = extractAllClaims(token);
            return !isExpired(token)
                    && expectedTokenType.equals(claims.get("tokenType", String.class))
                    && "tech-mel-api".equals(claims.getIssuer())
                    && "tech-mel-client".equals(claims.getAudience())
                    && isSignatureValid(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSignatureValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
