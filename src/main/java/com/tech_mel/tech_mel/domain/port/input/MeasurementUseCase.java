package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement.CreateMeasurementRequest;

public interface MeasurementUseCase {
    Measurement registerMeasurement(String apiKey, CreateMeasurementRequest request);
}
