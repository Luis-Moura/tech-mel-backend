package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.HiveEntity;
import org.springframework.stereotype.Component;

@Component
public class HiveMapper {
    private final UserMapper userMapper;

    public HiveMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Hive toDomain(HiveEntity entity) {
        if (entity == null) {
            return null;
        }

        return Hive.builder()
                .id(entity.getId())
                .name(entity.getName())
                .location(entity.getLocation())
                .apiKey(entity.getApiKey())
                .hiveStatus(entity.getHiveStatus())
                .owner(userMapper.toDomain(entity.getOwner()))
                .build();
    }

    public HiveEntity toEntity(Hive domain) {
        if (domain == null) {
            return null;
        }

        return HiveEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .location(domain.getLocation())
                .apiKey(domain.getApiKey())
                .hiveStatus(domain.getHiveStatus())
                .owner(userMapper.toEntity(domain.getOwner()))
                .build();
    }
}
