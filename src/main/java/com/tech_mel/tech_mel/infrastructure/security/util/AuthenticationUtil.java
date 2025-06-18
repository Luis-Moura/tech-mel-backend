package com.tech_mel.tech_mel.infrastructure.security.util;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.tech_mel.tech_mel.application.exception.UnauthorizedException;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationUtil {

    private final JwtPort jwtPort;

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Usuário não autenticado");
        }

        try {
            String userIdStr = authentication.getName();
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Token inválido: UUID malformado");
        }
    }

    public String getCurrentUserEmail() {
        String token = extractTokenFromRequest();
        
        if (token == null) {
            throw new UnauthorizedException("Token JWT não encontrado");
        }

        try {
            return jwtPort.extractUsername(token); // O subject do JWT é o email
        } catch (Exception e) {
            throw new UnauthorizedException("Erro ao extrair email do token: " + e.getMessage());
        }
    }

    public String getCurrentUserRole() {
        String token = extractTokenFromRequest();
        
        if (token == null) {
            throw new UnauthorizedException("Token JWT não encontrado");
        }

        try {
            Claims claims = jwtPort.extractAllClaims(token);
            return (String) claims.get("role");
        } catch (Exception e) {
            throw new UnauthorizedException("Erro ao extrair role do token: " + e.getMessage());
        }
    }

    public Claims getCurrentUserClaims() {
        String token = extractTokenFromRequest();
        
        if (token == null) {
            throw new UnauthorizedException("Token JWT não encontrado");
        }

        try {
            return jwtPort.extractAllClaims(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Erro ao extrair claims do token: " + e.getMessage());
        }
    }

    private String extractTokenFromRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attrs == null) {
            return null;
        }

        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
