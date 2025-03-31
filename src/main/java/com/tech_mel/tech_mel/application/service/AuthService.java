package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.InvalidCredentialsException;
import com.tech_mel.tech_mel.application.exception.TokenExpiredException;
import com.tech_mel.tech_mel.application.exception.UserNotFoundException;
import com.tech_mel.tech_mel.domain.event.UserRegisteredEvent;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AuthUseCase;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    //    private final JwtOperationsPort jwtOperationsPort;
    private final RefreshTokenUseCase refreshTokenUseCase;
//    private final JwtBlackListPort jwtBlackListPort;

    private final JwtPort jwtServicePort;

    @Override
    public String authenticateUser(String email, String password) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + email));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        if (!user.isEmailVerified()) {
            throw new InvalidCredentialsException("E-mail não verificado. Por favor, verifique seu e-mail antes de fazer login.");
        }

        if (!user.isEnabled() || user.isLocked()) {
            throw new InvalidCredentialsException("Conta bloqueada ou desativada. Entre em contato com o suporte.");
        }

        // Atualiza o último login
        user.setLastLogin(LocalDateTime.now());
        userRepositoryPort.save(user);

        // Gera o token JWT chamando a porta
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "ACCESS");

        return jwtServicePort.generateToken(claims, user.getEmail(), jwtExpiration); // 30 min
    }

    @Override
    public void registerUser(String email, String password, String name) {
        User existingUser = userRepositoryPort.findByEmail(email)
                .orElse(null);

        if (existingUser != null) {
            if (existingUser.isEmailVerified()) {
                throw new InvalidCredentialsException("E-mail já cadastrado");
            } else {
                existingUser.setPassword(passwordEncoder.encode(password));
                existingUser.setName(name);
                existingUser.setEnabled(true);
                existingUser.setLocked(false);

                String verificationToken = generateVerificationToken(existingUser);
                eventPublisher.publishEvent(new UserRegisteredEvent(existingUser, verificationToken));
                return;
            }
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .emailVerified(false)
                .role(User.Role.COMMON)
                .enabled(true)
                .locked(false)
                .build();

        user = userRepositoryPort.save(user);

        String verificationToken = generateVerificationToken(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user, verificationToken));
    }

    @Override
    public boolean verifyEmail(String token) {
        User user = userRepositoryPort.findByVerificationToken(token)
                .orElseThrow(() -> new TokenExpiredException("Token de verificação inválido"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token de verificação expirado");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepositoryPort.save(user);

        return true;
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + email));
    }

    @Override
    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepositoryPort.save(user);

        return token;
    }

    @Override
    public String refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenUseCase.verifyRefreshToken(refreshToken);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "ACCESS");

        return jwtServicePort.generateToken(claims, token.getUser().getEmail(), 30 * 60 * 1000L); // 30 min
    }

    @Override
    public void logout(String token) {
        if (!jwtServicePort.isTokenValid(token, "ACCESS")) {
            throw new TokenExpiredException("Token inválido ou expirado");
        }

        String userEmail = jwtServicePort.extractUsername(token);

        User user = userRepositoryPort.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        jwtServicePort.addToBlacklist(token);

        refreshTokenUseCase.revokeAllUserTokens(user);
    }
}