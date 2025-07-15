package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement.LatestHiveMeasurementResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.input.MeasurementUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement.CreateMeasurementRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement.CreateMeasurementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/measurements")
@RequiredArgsConstructor
@Tag(
        name = "Measurements",
        description = "Endpoints para comunicação com dispositivos IoT das colmeias e para opreções de medições"
)
public class MeasurementController {
    private final MeasurementUseCase measurementUseCase;
    private final AuthenticationUtil authenticationUtil;
    private final HiveRepositoryPort hiveRepositoryPort;

    @PostMapping("/iot")
    @Operation(
            summary = "Registra medições dos sensores",
            description = "Endpoint para dispositivos IoT enviarem dados de sensores das colmeias (temperatura, umidade e CO2)",
            security = @SecurityRequirement(name = "apiKey")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Medição registrada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateMeasurementResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos fornecidos",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "API Key inválida ou não fornecida",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada para a API Key fornecida",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<CreateMeasurementResponse> saveMeasurement(
            @Parameter(
                    description = "Chave API do dispositivo IoT da colmeia",
                    required = true,
                    example = "hive_12345_api_key_abcdef"
            )
            @RequestHeader("X-API-Key") String apiKey,

            @Parameter(
                    description = "Dados das medições dos sensores",
                    required = true
            )
            @Valid @RequestBody CreateMeasurementRequest request
    ) {
        Measurement measurement = measurementUseCase.registerMeasurement(apiKey, request);

        CreateMeasurementResponse response = CreateMeasurementResponse.builder()
                .temperature(measurement.getTemperature())
                .humidity(measurement.getHumidity())
                .co2(measurement.getCo2())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/latest/{hiveId}")
    public ResponseEntity<LatestHiveMeasurementResponse> getLatestMeasurementByHiveId(
            @PathVariable UUID hiveId
    ) {
        UUID userId = authenticationUtil.getCurrentUserId();

        Measurement latestMeasurement = measurementUseCase
                .getLatestMeasurementByApiKey(userId, hiveId);

        LatestHiveMeasurementResponse response = LatestHiveMeasurementResponse.builder()
                .hiveId(hiveId)
                .hiveName(hiveRepositoryPort.findById(hiveId)
                        .map(Hive::getName)
                        .orElse("Unknown Hive"))
                .latestMeasurement(latestMeasurement)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/latests")
    public ResponseEntity<List<LatestHiveMeasurementResponse>> getLatestMeasurements() {
        UUID userId = authenticationUtil.getCurrentUserId();

        Map<String, Measurement> latestMeasurements = measurementUseCase
                .getLatestMeasurementsGroupedByHive(userId);

        List<Hive> userHives = hiveRepositoryPort
                .findByOwnerId(userId, Pageable.unpaged()).getContent();

        List<LatestHiveMeasurementResponse> response = userHives.stream()
                .filter(hive -> latestMeasurements.containsKey(hive.getApiKey()))
                .map(hive -> LatestHiveMeasurementResponse.builder()
                        .hiveId(hive.getId())
                        .hiveName(hive.getName())
                        .latestMeasurement(latestMeasurements.get(hive.getApiKey()))
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }
}
