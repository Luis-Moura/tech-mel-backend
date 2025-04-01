package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.application.exception.UnauthorizedException;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.domain.port.output.RefreshTokenRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        return refreshTokenRepositoryPort.save(refreshToken);
    }

    @Override
    @Transactional()
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepositoryPort.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Refresh token not found"));

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Token de refresh expirado ou revogado");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepositoryPort.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Token de refresh não encontrado"));

        refreshTokenRepositoryPort.deleteByToken(refreshToken.getToken());
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepositoryPort.deleteByUser(user);
    }
}
