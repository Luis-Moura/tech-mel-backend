package com.tech_mel.tech_mel.infrastructure.security.filter;

import com.tech_mel.tech_mel.application.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ipAddress = getClientIP(request);
        String path = request.getRequestURI();

        Bucket bucket;

        if (isAuthenticationRequest(path)) {
            bucket = rateLimitService.getAuthBucket(ipAddress);
        } else {
            bucket = rateLimitService.getRegularBucket(ipAddress);
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("Muitas requisições. Tente novamente mais tarde.");
        }
    }

    private boolean isAuthenticationRequest(String path) {
        return path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register");
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader != null) {
            return xForwardedForHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
