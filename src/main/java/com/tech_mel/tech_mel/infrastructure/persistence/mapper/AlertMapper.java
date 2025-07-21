package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.Alert;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.AlertEntity;

public class AlertMapper {
    private final HiveMapper hiveMapper;

    public AlertMapper(HiveMapper hiveMapper) {
        this.hiveMapper = hiveMapper;
    }

    public AlertEntity toEntity(Alert domain) {
        if (domain == null) return null;
        return AlertEntity.builder()
                .id(domain.getId())
                .hive(hiveMapper.toEntity(domain.getHive()))
                .timestamp(domain.getTimestamp())
                .type(AlertEntity.AlertType.valueOf(domain.getType().name()))
                .severity(AlertEntity.AlertSeverity.valueOf(domain.getSeverity().name()))
                .value(domain.getValue())
                .status(AlertEntity.AlertStatus.valueOf(domain.getStatus().name()))
                .build();
    }

    public Alert toDomain(AlertEntity entity) {
        if (entity == null) return null;
        return Alert.builder()
                .id(entity.getId())
                .hive(hiveMapper.toDomain(entity.getHive()))
                .timestamp(entity.getTimestamp())
                .type(Alert.AlertType.valueOf(entity.getType().name()))
                .severity(Alert.AlertSeverity.valueOf(entity.getSeverity().name()))
                .value(entity.getValue())
                .status(Alert.AlertStatus.valueOf(entity.getStatus().name()))
                .build();
    }
}

