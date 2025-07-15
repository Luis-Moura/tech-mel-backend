package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.input.MeasurementUseCase;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement.CreateMeasurementRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement.CreateMeasurementResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement.DailyMeasurementAveragesResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement.LatestHiveMeasurementResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    @Operation(
        summary = "Buscar última medição de uma colmeia",
        description = "Retorna a última medição registrada para a colmeia informada.",
        security = @SecurityRequirement(name = "bearerAuth"),
        parameters = {
            @Parameter(
                name = "hiveId",
                description = "ID da colmeia",
                required = true,
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Última medição encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LatestHiveMeasurementResponse.class),
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\"hiveId\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"hiveName\": \"Colmeia Principal\", \"latestMeasurement\": {\"id\": \"c47ac10b-58cc-4372-a567-0e02b2c3d479\", \"temperature\": 34.2, \"humidity\": 78.5, \"co2\": 420.0, \"measuredAt\": \"2024-07-15T14:30:00\"}}"
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar a colmeia", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Colmeia ou medição não encontrada", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<LatestHiveMeasurementResponse> getLatestMeasurementByHiveId(
        @PathVariable UUID hiveId
    ) {
        UUID userId = authenticationUtil.getCurrentUserId();
        Measurement latestMeasurement = measurementUseCase.getLatestMeasurementByApiKey(userId, hiveId);
        LatestHiveMeasurementResponse response = LatestHiveMeasurementResponse.builder()
            .hiveId(hiveId)
            .hiveName(hiveRepositoryPort.findById(hiveId).map(Hive::getName).orElse("Unknown Hive"))
            .latestMeasurement(latestMeasurement)
            .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latests")
    @Operation(
        summary = "Buscar últimas medições de todas as colmeias do usuário",
        description = "Retorna a última medição registrada para cada colmeia do usuário autenticado.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de últimas medições por colmeia",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LatestHiveMeasurementResponse.class),
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "[{\"hiveId\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"hiveName\": \"Colmeia Principal\", \"latestMeasurement\": {\"id\": \"c47ac10b-58cc-4372-a567-0e02b2c3d479\", \"temperature\": 34.2, \"humidity\": 78.5, \"co2\": 420.0, \"measuredAt\": \"2024-07-15T14:30:00\"}}]"
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar a colmeia", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Usuário não possui colmeias ou não há medições", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<LatestHiveMeasurementResponse>> getLatestMeasurements() {
        UUID userId = authenticationUtil.getCurrentUserId();
        Map<String, Measurement> latestMeasurements = measurementUseCase.getLatestMeasurementsGroupedByHive(userId);
        List<Hive> userHives = hiveRepositoryPort.findByOwnerId(userId, Pageable.unpaged()).getContent();
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

    @GetMapping("daily-averages/{hiveId}")
    @Operation(
        summary = "Buscar médias diárias das medições de uma colmeia",
        description = "Retorna as médias diárias de temperatura, umidade e CO2 para a colmeia informada.",
        security = @SecurityRequirement(name = "bearerAuth"),
        parameters = {
            @Parameter(
                name = "hiveId",
                description = "ID da colmeia",
                required = true,
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
            ),
            @Parameter(
                name = "page",
                description = "Número da página de resultados",
                example = "0"
            ),
            @Parameter(
                name = "size",
                description = "Quantidade de itens por página",
                example = "10"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Página de médias diárias encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DailyMeasurementAveragesResponse.class),
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        value = "{\"content\": [{\"id\": \"c47ac10b-58cc-4372-a567-0e02b2c3d479\", \"avgTemperature\": 33.5, \"avgHumidity\": 75.2, \"avgCo2\": 410.0, \"date\": \"2024-07-15\", \"hiveId\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\"}], \"totalElements\": 1, \"totalPages\": 1, \"size\": 10, \"number\": 0}"
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar a colmeia", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Colmeia ou médias não encontradas", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Page<DailyMeasurementAveragesResponse>> getDailyMeasurementAveragesByHiveId(
        @PathVariable UUID hiveId,
        Pageable pageable
    ) {
        UUID userId = authenticationUtil.getCurrentUserId();
        Page<DailyMeasurementAverage> dailyAverages = measurementUseCase.getDailyMeasurementAverages(userId, hiveId, pageable);
        Page<DailyMeasurementAveragesResponse> response = dailyAverages
            .map(average -> DailyMeasurementAveragesResponse.builder()
                .id(average.getId())
                .avgTemperature(average.getAvgTemperature())
                .avgHumidity(average.getAvgHumidity())
                .avgCo2(average.getAvgCo2())
                .date(average.getDate())
                .hiveId(hiveId)
                .build());
        return ResponseEntity.ok(response);
    }
}
