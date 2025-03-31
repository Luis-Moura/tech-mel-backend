package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.port.output.RefreshTokenRepositoryPort;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.RefreshTokenEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.UserMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final RefreshTokenJpaRepository repository;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = refreshTokenMapper.toEntity(refreshToken);
        RefreshTokenEntity savedEntity = repository.save(entity);
        return refreshTokenMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    public List<RefreshToken> findByUser(User user) {
        return repository.findByUser(userMapper.toEntity(user))
                .stream()
                .map(refreshTokenMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }

    @Override
    public void deleteByUser(User user) {
        repository.deleteByUser(userMapper.toEntity(user));
    }
}