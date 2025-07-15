package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Measurement;

import java.util.List;
import java.util.Map;

public interface RedisIotPort {
    void saveMeasurement(String apiKey, Measurement measurement);

    List<Measurement> getMeasurements(String apiKey, int limit);

    Map<String, Measurement> getLatestMeasurementsForMultipleHives(List<String> apiKeys);

    Measurement getLatestMeasurement(String apiKey);

    void clearMeasurements(String apiKey);
}
