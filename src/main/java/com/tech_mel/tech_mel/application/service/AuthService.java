package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.InvalidCredentialsException;
import com.tech_mel.tech_mel.application.exception.InvalidTokenException;
import com.tech_mel.tech_mel.application.exception.UserNotFoundException;
import com.tech_mel.tech_mel.application.port.input.AuthUseCase;
import com.tech_mel.tech_mel.application.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.application.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.domain.event.UserRegisteredEvent;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
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
    private final RefreshTokenUseCase refreshTokenUseCase;

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
    public void registerUser(String email, String password, String name) {
        User existingUser = userRepository.findByEmail(email)
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

        user = userRepository.save(user);

        String verificationToken = generateVerificationToken(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user, verificationToken));

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
        RefreshToken token = refreshTokenUseCase.verifyRefreshToken(refreshToken);

        return jwtFactory.generateAccessToken(token.getUser());
    }
}