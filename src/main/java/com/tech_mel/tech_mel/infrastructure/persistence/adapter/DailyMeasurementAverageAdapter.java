package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import com.tech_mel.tech_mel.domain.port.output.DailyMeasurementAverageRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.DailyMeasurementAverageEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.DailyMeasurementAverageMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.DailyMeasurementAverageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DailyMeasurementAverageAdapter implements DailyMeasurementAverageRepositoryPort {

    private final DailyMeasurementAverageMapper dailyMeasurementAverageMapper;
    private final DailyMeasurementAverageRepository repository;

    @Override
    public DailyMeasurementAverage save(DailyMeasurementAverage dailyMeasurementAverage) {
        DailyMeasurementAverageEntity entity = dailyMeasurementAverageMapper.toEntity(dailyMeasurementAverage);
        DailyMeasurementAverageEntity savedEntity = repository.save(entity);
        return dailyMeasurementAverageMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<DailyMeasurementAverage> findById(UUID id) {
        return repository.findById(id)
                .map(dailyMeasurementAverageMapper::toDomain);
    }

    @Override
    public Page<DailyMeasurementAverage> findAllByHiveId(UUID hiveId, Pageable pageable) {
        Page<DailyMeasurementAverageEntity> dailyMeasurementAverageEntityPage = repository.findAllByHive_Id(hiveId, pageable);
        return dailyMeasurementAverageEntityPage.map(dailyMeasurementAverageMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
