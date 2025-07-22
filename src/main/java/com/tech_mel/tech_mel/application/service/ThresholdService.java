package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Threshold;
import com.tech_mel.tech_mel.domain.port.input.ThresholdUseCase;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.ThresholdRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.threshold.CreateThresholdRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdService implements ThresholdUseCase {
    private final ThresholdRepositoryPort thresholdRepositoryPort;
    private final HiveRepositoryPort hiveRepositoryPort;

    @Override
    public Threshold createThreshold(CreateThresholdRequest request, UUID ownerId) {
        Hive hive = hiveRepositoryPort.findById(request.hiveId())
                .orElseThrow(() -> {
                    log.error("Hive not found for ID: {}", request.hiveId());
                    return new NotFoundException("Hive not found for ID: " + request.hiveId());
                });

        if (!hive.getOwner().getId().equals(ownerId)) {
            log.error("Hive ID {} does not belong to owner ID {}", request.hiveId(), ownerId);
            throw new NotFoundException("Hive does not belong to the specified owner.");
        }

        Optional<Threshold> existingThreshold = thresholdRepositoryPort
                .findByHiveId(request.hiveId());

        if (existingThreshold.isPresent()) {
            log.error("Threshold already exists for Hive ID: {}", request.hiveId());
            throw new BadRequestException("Threshold already exists for this hive.");
        }

        Threshold threshold = Threshold.builder()
                .temperatureMin(request.temperatureMin())
                .temperatureMax(request.temperatureMax())
                .humidityMin(request.humidityMin())
                .humidityMax(request.humidityMax())
                .co2Min(request.co2Min())
                .co2Max(request.co2Max())
                .hive(hive)
                .build();

        return thresholdRepositoryPort.save(threshold);
    }

    @Override
    public Threshold getThresholdById(UUID thresholdId, UUID ownerId) {
        Threshold threshold = thresholdRepositoryPort.findById(thresholdId)
                .orElseThrow(() -> {
                    log.error("Threshold not found for ID: {}", thresholdId);
                    return new NotFoundException("Threshold not found for ID: " + thresholdId);
                });

        if (!threshold.getHive().getOwner().getId().equals(ownerId)) {
            log.error("Threshold ID {} does not belong to owner ID {}", thresholdId, ownerId);
            throw new NotFoundException("Threshold does not belong to the specified owner.");
        }

        return threshold;
    }

    @Override
    public Threshold getThresholdByHiveId(UUID hiveId, UUID ownerId) {
        Hive hive = hiveRepositoryPort.findById(hiveId)
                .orElseThrow(() -> {
                    log.error("Hive not found for ID: {}", hiveId);
                    return new NotFoundException("Hive not found for ID: " + hiveId);
                });

        if (!hive.getOwner().getId().equals(ownerId)) {
            log.error("Hive ID {} does not belong to owner ID {}", hiveId, ownerId);
            throw new NotFoundException("Hive does not belong to the specified owner.");
        }

        return thresholdRepositoryPort.findByHiveId(hiveId)
                .orElseThrow(() -> {
                    log.error("Threshold not found for Hive ID: {}", hiveId);
                    return new NotFoundException("Threshold not found for Hive ID: " + hiveId);
                });
    }

    @Override
    public void updateThreshold(UUID thresholdId, CreateThresholdRequest request, UUID ownerId) {
        Threshold threshold = thresholdRepositoryPort.findById(thresholdId)
                .orElseThrow(() -> {
                    log.error("Threshold not found for ID: {}", thresholdId);
                    return new NotFoundException("Threshold not found for ID: " + thresholdId);
                });

        if (!threshold.getHive().getOwner().getId().equals(ownerId)) {
            log.error("Threshold ID {} does not belong to owner ID {}", thresholdId, ownerId);
            throw new NotFoundException("Threshold does not belong to the specified owner.");
        }

        Hive hive = hiveRepositoryPort.findById(request.hiveId())
                .orElseThrow(() -> {
                    log.error("Hive not found for ID: {}", request.hiveId());
                    return new NotFoundException("Hive not found for ID: " + request.hiveId());
                });

        threshold.setTemperatureMin(request.temperatureMin());
        threshold.setTemperatureMax(request.temperatureMax());
        threshold.setHumidityMin(request.humidityMin());
        threshold.setHumidityMax(request.humidityMax());
        threshold.setCo2Min(request.co2Min());
        threshold.setCo2Max(request.co2Max());
        threshold.setHive(hive);

        thresholdRepositoryPort.save(threshold);
    }
}
