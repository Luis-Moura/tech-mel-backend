package com.tech_mel.tech_mel.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.tech_mel.tech_mel.application.exception.ConflictException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.application.exception.UnauthorizedException;
import com.tech_mel.tech_mel.domain.event.PasswordResetEvent;
import com.tech_mel.tech_mel.domain.event.UserRegisteredEvent;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AuthUseCase;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.output.JwtPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements AuthUseCase {

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final RedisTemplate<String, String> redisTemplate;

    private final JwtPort jwtPort;

    @Override
    public String authenticateUser(String email, String password) {
        try {
            User user = userRepositoryPort.findByEmail(email.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new NotFoundException("Credenciais inválidas"));

            if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
                log.warn("Tentativa de login com provedor não local: {}", email);
                throw new UnauthorizedException("Usuário não cadastrado com e-mail e senha");
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("Tentativa de login com senha inválida para: {}", email);
                throw new UnauthorizedException("Credenciais inválidas");
            }

            if (!user.isEmailVerified()) {
                log.warn("Tentativa de login com e-mail não verificado: {}", email);
                throw new UnauthorizedException("E-mail não verificado. Por favor, verifique seu e-mail antes de fazer login.");
            }

            if (!user.isEnabled() || user.isLocked()) {
                log.warn("Tentativa de login em conta bloqueada/desativada: {}", email);
                throw new UnauthorizedException("Conta bloqueada ou desativada. Entre em contato com o suporte.");
            }

            // Atualiza o último login
            user.setLastLogin(LocalDateTime.now());
            userRepositoryPort.save(user);

            log.info("Login bem-sucedido para: {}", email);

            // Gera o token JWT chamando a porta
            Map<String, Object> claims = new HashMap<>();
            claims.put("tokenType", "ACCESS");
            claims.put("userId", user.getId().toString());
            claims.put("role", user.getRole().name());

            return jwtPort.generateToken(claims, user.getEmail(), jwtExpiration); // 30 min
        } catch (Exception e) {
            log.error("Erro durante tentativa de login para {}: {}", email, e.getMessage());
            throw e;
        }
    }

    @Override
    public void registerUser(String email, String password, String name) {
        User existingUser = userRepositoryPort.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElse(null);

        if (existingUser != null) {
            if (existingUser.isEmailVerified()) {
                log.warn("Tentativa de registro com e-mail já cadastrado: {}", email);
                throw new ConflictException("E-mail já cadastrado");
            } else {
                existingUser.setPassword(passwordEncoder.encode(password));
                existingUser.setAuthProvider(User.AuthProvider.LOCAL);
                existingUser.setName(name);
                existingUser.setEnabled(true);
                existingUser.setLocked(false);

                String verificationToken = generateVerificationToken(existingUser);
                eventPublisher.publishEvent(new UserRegisteredEvent(existingUser, verificationToken));
                log.info("Reenvio de e-mail de verificação para usuário não verificado: {}", email);
                return;
            }
        }

        User user = User.builder()
                .email(email.toLowerCase(Locale.ROOT))
                .password(passwordEncoder.encode(password))
                .authProvider(User.AuthProvider.LOCAL)
                .name(name)
                .emailVerified(false)
                .role(User.Role.COMMON)
                .enabled(true)
                .locked(false)
                .build();

        user = userRepositoryPort.save(user);

        String verificationToken = generateVerificationToken(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user, verificationToken));
        log.info("Novo usuário registrado: {}", email);
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepositoryPort.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o email: " + email));

        if (user.isEmailVerified()) {
            log.warn("Tentativa de reenvio de verificação para e-mail já verificado: {}", email);
            throw new ConflictException("E-mail já verificado");
        }

        String verificationToken = generateVerificationToken(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(user, verificationToken));
        log.info("Reenvio de e-mail de verificação para: {}", email);
    }

    @Override
    public void verifyEmail(String token) {
        User user = userRepositoryPort.findByVerificationToken(token)
                .orElseThrow(() -> new UnauthorizedException("Token de verificação inválido ou expirado"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Token de verificação expirado para: {}", user.getEmail());
            throw new UnauthorizedException("Token de verificação inválido ou expirado");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepositoryPort.save(user);
        log.info("E-mail verificado com sucesso: {}", user.getEmail());
    }

    @Override
    public String refreshToken(String refreshToken) {
        try {
            RefreshToken token = refreshTokenUseCase.verifyRefreshToken(refreshToken);

            Map<String, Object> claims = new HashMap<>();
            claims.put("tokenType", "ACCESS");
            claims.put("userId", token.getUser().getId().toString());
            claims.put("role", token.getUser().getRole().name());

            log.info("Refresh token bem-sucedido para usuário: {}", token.getUser().getEmail());
            return jwtPort.generateToken(claims, token.getUser().getEmail(), 30 * 60 * 1000L); // 30 min
        } catch (Exception e) {
            log.warn("Falha ao renovar token: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void logout(String token) {
        if (!jwtPort.isTokenValid(token, "ACCESS")) {
            log.warn("Tentativa de logout com token inválido");
            throw new UnauthorizedException("Token inválido ou expirado");
        }

        String userEmail = jwtPort.extractUsername(token);

        User user = userRepositoryPort.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        jwtPort.addToBlacklist(token);
        refreshTokenUseCase.revokeAllUserTokens(user);
        log.info("Logout realizado para: {}", userEmail);
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userRepositoryPort.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o email: " + email));

        if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            log.warn("Tentativa de reset de senha para conta Google: {}", email);
            throw new UnauthorizedException("Usuário não cadastrado com e-mail e senha");
        }

        UUID resetToken = UUID.randomUUID();
        String redisKey = "password-reset:" + resetToken;

        // Armazenando o userId como String
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), Duration.ofMinutes(15));

        eventPublisher.publishEvent(new PasswordResetEvent(user.getEmail(), user.getName(), resetToken));
        log.info("Solicitação de reset de senha para: {}", email);
    }

    @Override
    public void resetPassword(UUID token, String newPassword) {
        String redisKey = "password-reset:" + token.toString();
        String userIdStr = redisTemplate.opsForValue().get(redisKey);

        if (userIdStr == null) {
            log.warn("Token de redefinição de senha inválido ou expirado: {}", token);
            throw new UnauthorizedException("Token de redefinição de senha inválido ou expirado");
        }

        UUID userId;

        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Token de redefinição de senha mal formatado: {}", token);
            throw new UnauthorizedException("Token de redefinição de senha inválido");
        }

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            log.warn("Tentativa de redefinir senha para conta Google: {}", user.getEmail());
            throw new UnauthorizedException("Usuário não cadastrado com e-mail e senha");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepositoryPort.save(user);

        redisTemplate.delete(redisKey);
        log.info("Senha redefinida com sucesso para: {}", user.getEmail());
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepositoryPort.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com o email: " + email));
    }

    @Override
    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepositoryPort.save(user);

        return token;
    }
}