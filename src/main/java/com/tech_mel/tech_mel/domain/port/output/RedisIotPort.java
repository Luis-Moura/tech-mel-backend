package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Measurement;

import java.util.List;

public interface RedisIotPort {
    void saveMeasurement(String apiKey, Measurement measurement);

    List<Measurement> getMeasurements(String apiKey, int limit);

    Measurement getLatestMeasurement(String apiKey);

    void clearMeasurements(String apiKey);
}
