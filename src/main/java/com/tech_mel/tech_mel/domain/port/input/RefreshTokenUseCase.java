package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;

public interface RefreshTokenUseCase {
    RefreshToken createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    void revokeRefreshToken(String token);

    void revokeAllUserTokens(User user);
}