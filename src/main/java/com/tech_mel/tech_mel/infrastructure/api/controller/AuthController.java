package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.port.input.AuthUseCase;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.AuthRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.AuthResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.RefreshTokenRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.RegistrationRequest;
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
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String accessToken = authUseCase.authenticateUser(request.getEmail(), request.getPassword());

        User user = authUseCase.findUserByEmail(request.getEmail());
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
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        authUseCase.registerUser(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.ok().body(Map.of("message", "Usuário registrado com sucesso. Verifique seu e-mail para ativar a conta."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        String newAccessToken = authUseCase.refreshToken(request.getRefreshToken());

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        boolean verified = authUseCase.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok().body(Map.of("message", "E-mail verificado com sucesso. Agora você pode fazer login."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Falha ao verificar o e-mail."));
        }
    }
}