package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.input.IotUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.iot.CreateMeasurementRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.iot.CreateMeasurementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot/measurements")
@RequiredArgsConstructor
public class IotController {
    private final IotUseCase iotUseCase;

    @PostMapping()
    public ResponseEntity<CreateMeasurementResponse> saveMeasurement(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody CreateMeasurementRequest request
    ) {
        Measurement measurement = iotUseCase.registerMeasurement(apiKey, request);

        CreateMeasurementResponse response = CreateMeasurementResponse.builder()
                .temperature(measurement.getTemperature())
                .humidity(measurement.getHumidity())
                .co2(measurement.getCo2())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
