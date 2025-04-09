package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.application.exception.UnauthorizedException;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AuthUseCase;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.AuthRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.RefreshTokenRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.RegistrationRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.ResendEmailVerificationRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        String accessToken = authUseCase.authenticateUser(request.email(), request.password());

        User user = authUseCase.findUserByEmail(request.email());
        RefreshToken refreshToken = refreshTokenUseCase.createRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        authUseCase.registerUser(request.email(), request.password(), request.name());
        return ResponseEntity.ok().body(Map.of("message", "Usuário registrado com sucesso. Verifique seu e-mail para ativar a conta."));
    }

    @PostMapping("/resend-verification-email")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestParam ResendEmailVerificationRequest request) {
        authUseCase.resendVerificationEmail(request.email());
        return ResponseEntity.ok().body(Map.of("message", "E-mail de verificação reenviado com sucesso."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = authUseCase.refreshToken(request.refreshToken());

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.refreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        authUseCase.verifyEmail(token);

        return ResponseEntity.ok().body(Map.of("message", "E-mail verificado com sucesso. Agora você pode fazer login."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        // Valida o formato do cabeçalho de autorização
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Cabeçalho de autorização inválido");
        }

        String token = authHeader.substring(7);
        authUseCase.logout(token);

        return ResponseEntity.ok().body(Map.of("message", "Logout realizado com sucesso"));
    }
}