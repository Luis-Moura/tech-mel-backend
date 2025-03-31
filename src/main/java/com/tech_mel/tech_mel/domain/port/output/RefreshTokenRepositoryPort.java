package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(User user);

    void deleteByToken(String token);

    void deleteByUser(User user);
}