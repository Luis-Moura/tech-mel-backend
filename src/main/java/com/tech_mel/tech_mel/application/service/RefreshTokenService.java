package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.application.port.output.RefreshTokenRepositoryPort;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenExpiration;

    @Override
    public RefreshToken createRefreshToken(User user) {
        return null;
    }

    @Override
    public RefreshToken verifyRefreshToken(String token) {
        return null;
    }

    @Override
    public void revokeRefreshToken(String token) {

    }

    @Override
    public void revokeAllUserTokens(User user) {

    }
}
