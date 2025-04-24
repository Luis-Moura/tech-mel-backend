package com.tech_mel.tech_mel.infrastructure.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech_mel.tech_mel.application.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(
            @SuppressWarnings("null") @NotNull HttpServletRequest request,
            @SuppressWarnings("null") @NotNull HttpServletResponse response,
            @SuppressWarnings("null") @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ipAddress = getClientIP(request);
        String path = request.getRequestURI();

        Bucket bucket;

        if (isResetPasswordRequest(path)) {
            bucket = rateLimitService.getResetPasswordBucket(ipAddress);
        } else if (isAuthenticationRequest(path)) {
            bucket = rateLimitService.getAuthBucket(ipAddress);
        } else {
            bucket = rateLimitService.getRegularBucket(ipAddress);
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            // Em vez de lançar exceção, respondemos diretamente ao cliente
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            errorDetails.put("timestamp", LocalDateTime.now().toString());
            errorDetails.put("message", "Muitas requisições. Tente novamente mais tarde.");
            errorDetails.put("error", "Too Many Requests");

            objectMapper.writeValue(response.getWriter(), errorDetails);
            response.flushBuffer();
        }
    }

    private boolean isAuthenticationRequest(String path) {
        return path.startsWith("/api/auth/") && !isResetPasswordRequest(path);
    }

    private boolean isResetPasswordRequest(String path) {
        return path.startsWith("/api/auth/reset-password") ||
                path.startsWith("/api/auth/forgot-password");
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
