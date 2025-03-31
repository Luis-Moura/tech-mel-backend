package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {
    private final UserMapper userMapper;

    public RefreshTokenMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return RefreshToken.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .user(userMapper.toDomain(entity.getUser()))
                .expiryDate(entity.getExpiryDate())
                .build();
    }

    public RefreshTokenEntity toEntity(RefreshToken domain) {
        if (domain == null) {
            return null;
        }

        return RefreshTokenEntity.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .user(userMapper.toEntity(domain.getUser()))
                .expiryDate(domain.getExpiryDate())
                .build();
    }
}
