package com.tech_mel.tech_mel.infrastructure.security.filter;

import com.tech_mel.tech_mel.application.exception.TokenExpiredException;
import com.tech_mel.tech_mel.application.service.JwtValidationService;
import com.tech_mel.tech_mel.domain.port.output.JwtOperationsPort;
import com.tech_mel.tech_mel.infrastructure.security.auth.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;
    private final JwtOperationsPort jwtOperationsPort;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            if (!jwtValidationService.validateToken(jwt, "ACCESS")) {
                throw new TokenExpiredException("Token inválido ou expirado");
            }

            userEmail = jwtOperationsPort.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (TokenExpiredException e) {
            SecurityContextHolder.clearContext();

            request.setAttribute("exception", e);

            throw e;
        } catch (Exception e) {
            SecurityContextHolder.clearContext();

            request.setAttribute("exception", e);

            throw new TokenExpiredException(e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}