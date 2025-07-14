package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.DailyMeasurementAverageEntity;
import org.springframework.stereotype.Component;

@Component
public class DailyMeasurementAverageMapper {
    private final HiveMapper hiveMapper;

    public DailyMeasurementAverageMapper(HiveMapper hiveMapper) {
        this.hiveMapper = hiveMapper;
    }

    public DailyMeasurementAverage toDomain(DailyMeasurementAverageEntity entity) {
        if (entity == null) {
            return null;
        }

        return DailyMeasurementAverage.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .avgTemperature(entity.getAvgTemperature())
                .avgHumidity(entity.getAvgHumidity())
                .avgCo2(entity.getAvgCo2())
                .hive(hiveMapper.toDomain(entity.getHive()))
                .build();
    }

    public DailyMeasurementAverageEntity toEntity(DailyMeasurementAverage domain) {
        if (domain == null) {
            return null;
        }

        return DailyMeasurementAverageEntity.builder()
                .id(domain.getId())
                .date(domain.getDate())
                .avgTemperature(domain.getAvgTemperature())
                .avgHumidity(domain.getAvgHumidity())
                .avgCo2(domain.getAvgCo2())
                .hive(hiveMapper.toEntity(domain.getHive()))
                .build();
    }
}
