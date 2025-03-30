package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.InvalidCredentialsException;
import com.tech_mel.tech_mel.application.exception.InvalidTokenException;
import com.tech_mel.tech_mel.application.exception.UserNotFoundException;
import com.tech_mel.tech_mel.application.port.input.AuthUseCase;
import com.tech_mel.tech_mel.application.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.domain.event.UserRegisteredEvent;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.infrastructure.security.jwt.JwtFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtFactory jwtFactory;

    @Override
    public String authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
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
        userRepository.save(user);

        return jwtFactory.generateAccessToken(user);
    }

    @Override
    public User registerUser(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new InvalidCredentialsException("E-mail já cadastrado");
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

        user = userRepository.save(user);

        String verificationToken = generateVerificationToken(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user, verificationToken));

        return user;
    }

    @Override
    public boolean verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Token de verificação inválido"));

        if (user.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Token de verificação expirado");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);

        return true;
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o email: " + email));
    }

    @Override
    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();

        user.setVerificationToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        return token;
    }

    @Override
    public String refreshToken(String refreshToken) {
        if (!jwtFactory.validateToken(refreshToken) || !jwtFactory.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Refresh token inválido ou expirado");
        }

        String email = jwtFactory.extractUsername(refreshToken);
        UUID userId = jwtFactory.extractUserId(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (!user.getId().equals(userId)) {
            throw new InvalidTokenException("Token inválido");
        }

        if (!user.isEnabled() || user.isLocked()) {
            throw new InvalidCredentialsException("Conta bloqueada ou desativada");
        }

        return jwtFactory.generateAccessToken(user);
    }
}