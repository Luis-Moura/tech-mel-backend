package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement.CreateMeasurementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface MeasurementUseCase {
    Measurement registerMeasurement(String apiKey, CreateMeasurementRequest request);

    Measurement getLatestMeasurementByApiKey(UUID userId, UUID hiveId);

    Map<String, Measurement> getLatestMeasurementsGroupedByHive(UUID userId);

    Page<DailyMeasurementAverage> getDailyMeasurementAverages(UUID userId, UUID hiveId, Pageable pageable);
}
