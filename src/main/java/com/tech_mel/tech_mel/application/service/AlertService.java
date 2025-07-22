package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.Alert;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.model.Threshold;
import com.tech_mel.tech_mel.domain.port.input.AlertUseCase;
import com.tech_mel.tech_mel.domain.port.output.AlertRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.ThresholdRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService implements AlertUseCase {

    private final AlertRepositoryPort alertRepositoryPort;
    private final ThresholdRepositoryPort thresholdRepositoryPort;
    private final HiveRepositoryPort hiveRepositoryPort;

    @Override
    public Alert saveAlert(Measurement measurement, Hive hive, LocalDateTime timestamp) {
        Threshold threshold = thresholdRepositoryPort.findByHiveId(hive.getId())
                .orElseThrow(() -> new NotFoundException("Threshold not configured"));

        List<Alert> alerts = new ArrayList<>();

        if (measurement.getTemperature() < threshold.getTemperatureMin() || measurement.getTemperature() > threshold.getTemperatureMax()) {
            alerts.add(buildAlert(hive, timestamp, Alert.AlertType.TEMPERATURE, measurement.getTemperature(), threshold));
        }

        if (measurement.getHumidity() < threshold.getHumidityMin() || measurement.getHumidity() > threshold.getHumidityMax()) {
            alerts.add(buildAlert(hive, timestamp, Alert.AlertType.HUMIDITY, measurement.getHumidity(), threshold));
        }

        if (measurement.getCo2() < threshold.getCo2Min() || measurement.getCo2() > threshold.getCo2Max()) {
            alerts.add(buildAlert(hive, timestamp, Alert.AlertType.CO2, measurement.getCo2(), threshold));
        }

        alerts.forEach(alertRepositoryPort::save);
        return alerts.isEmpty() ? null : alerts.get(0);
    }

    @Override
    public Alert getAlertById(UUID alertId, UUID ownerId) {
        return alertRepositoryPort.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert not found with ID: " + alertId));
    }

    @Override
    public Page<Alert> getAlertsByHiveIdAndStatus(
            UUID hiveId,
            Alert.AlertStatus status,
            UUID ownerId,
            Pageable pageable
    ) {
        Hive hive = hiveRepositoryPort.findById(hiveId)
                .orElseThrow(() -> new NotFoundException("Hive not found with ID: " + hiveId));

        if (!hive.getOwner().getId().equals(ownerId)) {
            throw new BadRequestException("Hive does not belong to the owner");
        }

        if (status == null) {
            return alertRepositoryPort.findAllByHiveId(hiveId, pageable);
        }

        return alertRepositoryPort.findAllByHiveIdAndStatus(hiveId, status, pageable);
    }

    @Override
    public void updateAlertStatus(UUID alertId, Alert.AlertStatus status, UUID ownerId) {
        Alert alert = alertRepositoryPort.findById(alertId)
                .orElseThrow(() -> new NotFoundException("Alert not found with ID: " + alertId));

        Hive hive = hiveRepositoryPort.findById(alert.getHive().getId())
                .orElseThrow(() -> new NotFoundException("Hive not found with ID: " + alert.getHive().getId()));

        if (!hive.getOwner().getId().equals(ownerId)) {
            throw new BadRequestException("Hive does not belong to the owner");
        }

        alert.setStatus(status);
        alertRepositoryPort.save(alert);
    }

    private Alert buildAlert(Hive hive, LocalDateTime timestamp, Alert.AlertType type, Double value, Threshold threshold) {
        return Alert.builder()
                .hive(hive)
                .timestamp(timestamp)
                .type(type)
                .severity(calculateSeverity(type, value, threshold))
                .value(value)
                .status(Alert.AlertStatus.NEW)
                .build();
    }

    private Alert.AlertSeverity calculateSeverity(Alert.AlertType type, Double value, Threshold threshold) {
        double min, max;

        switch (type) {
            case TEMPERATURE:
                min = threshold.getTemperatureMin();
                max = threshold.getTemperatureMax();
                break;
            case HUMIDITY:
                min = threshold.getHumidityMin();
                max = threshold.getHumidityMax();
                break;
            case CO2:
                min = threshold.getCo2Min();
                max = threshold.getCo2Max();
                break;
            default:
                throw new BadRequestException("Unknown alert type: " + type);
        }

        if (value >= min && value <= max) {
            return null; // Sem alerta — valor dentro do intervalo
        }

        double distance = value < min ? min - value : value - max;
        double range = max - min;
        double percentage = (range == 0) ? 100 : (distance / range) * 100; // proteção contra divisão por zero

        if (percentage <= 10) {
            return Alert.AlertSeverity.LOW;
        } else if (percentage <= 30) {
            return Alert.AlertSeverity.MEDIUM;
        } else {
            return Alert.AlertSeverity.HIGH;
        }
    }
}
