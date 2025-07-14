package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement.CreateMeasurementRequest;

public interface MeasurementUseCase {
    Measurement registerMeasurement(String apiKey, CreateMeasurementRequest request);

    Measurement getLastMeasurement(String apiKey);

    // criar o metodo de fazer a média de medidas das ultimas 24 horas
    // pegar do redis e salvar a média no postgres
}
