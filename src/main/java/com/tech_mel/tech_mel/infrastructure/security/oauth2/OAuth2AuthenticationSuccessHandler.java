package com.tech_mel.tech_mel.infrastructure.security.oauth2;

import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtPort jwtPort;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final UserRepositoryPort userRepository;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isEmpty()) {
            String name = oAuth2User.getAttribute("name");
            String sub = oAuth2User.getAttribute("sub");

            user = User.builder()
                    .email(email)
                    .name(name != null ? name : "Usuário Google")
                    .password("")
                    .providerId(sub)
                    .authProvider(User.AuthProvider.GOOGLE)
                    .emailVerified(true)
                    .enabled(true)
                    .role(User.Role.COMMON)
                    .build();

            user = userRepository.save(user);
        } else {
            user = userOptional.get();
            if (user.getAuthProvider() == User.AuthProvider.LOCAL) {
                String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                        .queryParam("error", "Você já possui uma conta registrada com email e senha. Por favor, use essa forma de login.")
                        .build().toUriString();
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }

        if (user.getRole() != User.Role.COMMON) {
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "Apenas usuários comuns podem usar o login com Google.")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // Gerar tokens JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("tokenType", "ACCESS");

        String accessToken = jwtPort.generateToken(claims, user.getEmail(), jwtExpiration);
        RefreshToken refreshToken = refreshTokenUseCase.createRefreshToken(user);

        // Gerar um ID de estado único
        String stateId = UUID.randomUUID().toString();

        // Armazenar tokens temporariamente no Redis (120 segundos)
        TokenPair tokenPair = new TokenPair(accessToken, refreshToken.getToken());
        objectRedisTemplate.opsForValue().set(
                "oauth2:state:" + stateId,
                tokenPair,
                120,
                TimeUnit.SECONDS
        );

        // Redirecionar com apenas o stateId na URL
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("state", stateId)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}