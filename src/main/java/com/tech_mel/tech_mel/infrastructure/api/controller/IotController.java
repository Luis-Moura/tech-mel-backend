package com.tech_mel.tech_mel.infrastructure.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.input.IotUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.iot.CreateMeasurementRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.iot.CreateMeasurementResponse;
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

@RestController
@RequestMapping("/api/iot/measurements")
@RequiredArgsConstructor
@Tag(name = "IoT", description = "Endpoints para comunicação com dispositivos IoT das colmeias")
public class IotController {
    private final IotUseCase iotUseCase;

    @PostMapping()
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
        Measurement measurement = iotUseCase.registerMeasurement(apiKey, request);

        CreateMeasurementResponse response = CreateMeasurementResponse.builder()
                .temperature(measurement.getTemperature())
                .humidity(measurement.getHumidity())
                .co2(measurement.getCo2())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
