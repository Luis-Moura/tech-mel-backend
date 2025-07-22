package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.Threshold;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.ThresholdEntity;
import org.springframework.stereotype.Component;

@Component
public class ThresholdMapper {
    private final HiveMapper hiveMapper;

    public ThresholdMapper(HiveMapper hiveMapper) {
        this.hiveMapper = hiveMapper;
    }

    public ThresholdEntity toEntity(Threshold domain) {
        if (domain == null) return null;

        return ThresholdEntity.builder()
                .id(domain.getId())
                .temperatureMin(domain.getTemperatureMin())
                .temperatureMax(domain.getTemperatureMax())
                .humidityMin(domain.getHumidityMin())
                .humidityMax(domain.getHumidityMax())
                .co2Min(domain.getCo2Min())
                .co2Max(domain.getCo2Max())
                .hive(hiveMapper.toEntity(domain.getHive()))
                .build();
    }

    public Threshold toDomain(ThresholdEntity entity) {
        if (entity == null) return null;
        return Threshold.builder()
                .id(entity.getId())
                .temperatureMin(entity.getTemperatureMin())
                .temperatureMax(entity.getTemperatureMax())
                .humidityMin(entity.getHumidityMin())
                .humidityMax(entity.getHumidityMax())
                .co2Min(entity.getCo2Min())
                .co2Max(entity.getCo2Max())
                .hive(hiveMapper.toDomain(entity.getHive()))
                .build();
    }
}

