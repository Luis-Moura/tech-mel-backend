package com.tech_mel.tech_mel.infrastructure.security.oauth2;

import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtPort jwtPort;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final UserRepositoryPort userRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String sub = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Criar um novo usuário caso não exista
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .password("")
                            .providerId(sub)
                            .authProvider(User.AuthProvider.GOOGLE)
                            .emailVerified(true)
                            .enabled(true)
                            .role(User.Role.COMMON)
                            .build();
                    return userRepository.save(newUser);
                });

        // Gerar token JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("tokenType", "ACCESS");

        String accessToken = jwtPort.generateToken(claims, user.getEmail(), jwtExpiration);

        // Gerar refresh token
        RefreshToken refreshToken = refreshTokenUseCase.createRefreshToken(user);

        // Construir URL de redirecionamento com tokens
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}