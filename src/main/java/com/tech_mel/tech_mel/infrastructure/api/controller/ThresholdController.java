package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Threshold;
import com.tech_mel.tech_mel.domain.port.input.ThresholdUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.threshold.CreateThresholdRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.threshold.ThresholdResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hives/thresholds")
@RequiredArgsConstructor
@Tag(name = "Thresholds", description = "Endpoints para gestão de limiares de sensores")
@SecurityRequirement(name = "bearerAuth")
public class ThresholdController {
    private final ThresholdUseCase thresholdUseCase;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping
    @Operation(
            summary = "Criar limiar de sensores",
            description = "Cria um novo limiar de sensores para uma colmeia do usuário autenticado. Retorna os dados do limiar criado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Limiar criado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ThresholdResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                                                "temperatureMin": 10.0,
                                                "temperatureMax": 40.0,
                                                "humidityMin": 30.0,
                                                "humidityMax": 80.0,
                                                "co2Min": 350.0,
                                                "co2Max": 1200.0
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 400,
                                                "error": "Validation Error",
                                                "message": {
                                                    "temperatureMin": "Temperatura mínima obrigatória",
                                                    "hiveId": "ID da colmeia obrigatório"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Colmeia não encontrada"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<ThresholdResponse> createThreshold(CreateThresholdRequest request) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        Threshold threshold = thresholdUseCase.createThreshold(request, ownerId);

        ThresholdResponse response = ThresholdResponse.builder()
                .id(threshold.getId())
                .temperatureMin(threshold.getTemperatureMin())
                .temperatureMax(threshold.getTemperatureMax())
                .humidityMin(threshold.getHumidityMin())
                .humidityMax(threshold.getHumidityMax())
                .co2Min(threshold.getCo2Min())
                .co2Max(threshold.getCo2Max())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{thresholdId}")
    @Operation(
            summary = "Buscar limiar por ID",
            description = "Retorna os dados de um limiar de sensores específico pelo seu ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Limiar encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ThresholdResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
                                                "temperatureMin": 10.0,
                                                "temperatureMax": 40.0,
                                                "humidityMin": 30.0,
                                                "humidityMax": 80.0,
                                                "co2Min": 350.0,
                                                "co2Max": 1200.0
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão para acessar este limiar",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Você não tem permissão para acessar este limiar."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Limiar não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Limiar não encontrado"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<ThresholdResponse> getThresholdById(@PathVariable UUID thresholdId) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        Threshold threshold = thresholdUseCase.getThresholdById(thresholdId, ownerId);

        ThresholdResponse response = ThresholdResponse.builder()
                .id(threshold.getId())
                .temperatureMin(threshold.getTemperatureMin())
                .temperatureMax(threshold.getTemperatureMax())
                .humidityMin(threshold.getHumidityMin())
                .humidityMax(threshold.getHumidityMax())
                .co2Min(threshold.getCo2Min())
                .co2Max(threshold.getCo2Max())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{thresholdId}")
    @Operation(
            summary = "Atualizar limiar de sensores",
            description = "Atualiza os dados de um limiar de sensores existente pelo seu ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Limiar atualizado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 400,
                                                "error": "Validation Error",
                                                "message": {
                                                    "temperatureMin": "Temperatura mínima obrigatória"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão para atualizar este limiar",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Você não tem permissão para atualizar este limiar."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Limiar não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Limiar não encontrado"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Void> updateThreshold(
            @PathVariable UUID thresholdId,
            @RequestBody CreateThresholdRequest request
    ) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        thresholdUseCase.updateThreshold(thresholdId, request, ownerId);

        return ResponseEntity.noContent().build();
    }
}
