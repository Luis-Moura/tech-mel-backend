package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DailyMeasurementAverageRepositoryPort {
    DailyMeasurementAverage save(DailyMeasurementAverage dailyMeasurementAverage);

    Optional<DailyMeasurementAverage> findById(UUID id);

    Page<DailyMeasurementAverage> findAllByHiveId(UUID hiveId, Pageable pageable);

    void deleteById(UUID id);
}
