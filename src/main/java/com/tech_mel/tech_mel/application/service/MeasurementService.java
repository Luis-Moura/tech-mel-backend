package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.ConflictException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.input.AlertUseCase;
import com.tech_mel.tech_mel.domain.port.input.MeasurementUseCase;
import com.tech_mel.tech_mel.domain.port.output.DailyMeasurementAverageRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.RedisIotPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement.CreateMeasurementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementService implements MeasurementUseCase {
    private final HiveRepositoryPort hiveRepositoryPort;
    private final DailyMeasurementAverageRepositoryPort dailyMeasurementAverageRepositoryPort;
    private final AlertUseCase alertUseCase;
    private final RedisIotPort redisIotPort;

    @Override
    public Measurement registerMeasurement(String apiKey, CreateMeasurementRequest request) {
        Hive hive = hiveRepositoryPort.findByApiKey(apiKey)
                .orElseThrow(() -> new NotFoundException("Hive not found for API key: " + apiKey));

        if (hive.getHiveStatus() == Hive.HiveStatus.INACTIVE) {
            log.warn("Attempt to register measurement for inactive hive: {}", hive.getId());
            throw new ConflictException("Cannot register measurement for an inactive hive.");
        }

        log.info("Registering measurement for hive: {}", hive.getId());

        Measurement measurement = Measurement.builder()
                .id(UUID.randomUUID())
                .temperature(request.temperature())
                .humidity(request.humidity())
                .co2(request.co2())
                .measuredAt(request.measuredAt())
                .build();

        redisIotPort.saveMeasurement(apiKey, measurement);

        alertUseCase.saveAlert(measurement, hive, request.measuredAt());

        return measurement;
    }

    @Override
    public Measurement getLatestMeasurementByApiKey(UUID userId, UUID hiveId) {
        Hive hive = hiveRepositoryPort.findById(hiveId)
                .filter(h -> h.getOwner().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Hive not found"));

        Measurement lastestMeasurement = redisIotPort.getLatestMeasurement(hive.getApiKey());

        if (lastestMeasurement == null) {
            log.warn("No measurements found for hive: {}", hive.getId());
            throw new NotFoundException("No measurements found for the given hive.");
        }

        return lastestMeasurement;
    }

    @Override
    public Map<String, Measurement> getLatestMeasurementsGroupedByHive(UUID userId) {
        List<Hive> hives = hiveRepositoryPort.findByOwnerId(userId, Pageable.unpaged())
                .getContent();

        if (hives.isEmpty()) {
            log.warn("No hives found for user: {}", userId);
            throw new NotFoundException("No hives found for the given user.");
        }

        List<String> apiKeys = hives.stream()
                .map(Hive::getApiKey)
                .filter(Objects::nonNull)
                .toList();

        if (apiKeys.isEmpty()) {
            log.warn("No API keys found for user: {}", userId);
            throw new NotFoundException("No API keys found for the given user.");
        }

        return redisIotPort.getLatestMeasurementsForMultipleHives(apiKeys);
    }

    @Override
    public Page<DailyMeasurementAverage> getDailyMeasurementAverages(
            UUID userId,
            UUID hived,
            Pageable pageable
    ) {
        Hive hive = hiveRepositoryPort.findById(hived)
                .orElseThrow(() -> new NotFoundException("Hive not found"));

        if (hive.getOwner().getId() != userId) {
            log.warn("User {} does not own hive: {}", userId, hive.getId());
            throw new NotFoundException("Hive not found for the given user.");
        }

        Page<DailyMeasurementAverage> dailyAverages = dailyMeasurementAverageRepositoryPort
                .findAllByHiveId(hive.getId(), pageable);

        if (dailyAverages.isEmpty()) {
            log.warn("No daily measurement averages found for hive: {}", hive.getId());
            throw new NotFoundException("No daily measurement averages found for the given hive.");
        }

        return dailyAverages;
    }
}
