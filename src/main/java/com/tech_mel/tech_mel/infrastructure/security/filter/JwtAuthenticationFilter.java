package com.tech_mel.tech_mel.infrastructure.security.filter;

import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tech_mel.tech_mel.application.exception.UnauthorizedException;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtPort jwtServicePort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    protected void doFilterInternal(
            @SuppressWarnings("null") @NotNull HttpServletRequest request,
            @SuppressWarnings("null") @NotNull HttpServletResponse response,
            @SuppressWarnings("null") @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            String username = jwtServicePort.extractUsername(jwt);
            User user = userRepositoryPort.findByEmail(username)
                    .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

            if (!user.isEnabled()) {
                log.warn("Token de usuário desabilitado: {}", username);
                throw new UnauthorizedException("Token inválido ou expirado");
            }

            if (!jwtServicePort.isTokenValid(jwt, "ACCESS")) {
                log.warn("Token JWT inválido ou expirado para: {}", username);
                throw new UnauthorizedException("Token inválido ou expirado");
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var claims = jwtServicePort.extractAllClaims(jwt);
                String role = (String) claims.get("role");

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("JWT autenticado com sucesso para: {}", username);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Falha na autenticação JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            request.setAttribute("exception", e);
            throw new UnauthorizedException("Erro de autenticação: " + e.getMessage());
        }
    }
}