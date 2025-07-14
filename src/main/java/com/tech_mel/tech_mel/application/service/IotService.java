package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.application.exception.ConflictException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.input.IotUseCase;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.RedisIotPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.iot.CreateMeasurementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IotService implements IotUseCase {
    private final HiveRepositoryPort hiveRepositoryPort;
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

        // disparar alerta para caso medição esteja fora dos limites,
        // necessário implementar lógica de alerta com serviço e portas e etc.

        return measurement;
    }
}
