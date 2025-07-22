package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Alert;
import com.tech_mel.tech_mel.domain.port.input.AlertUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.alert.AlertResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Endpoints para gestão de alertas")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {
    private final AlertUseCase alertUseCase;
    private final AuthenticationUtil authenticationUtil;

    @Operation(
            summary = "Buscar alerta por ID",
            description = "Retorna um alerta específico pelo seu ID. Requer autenticação.",
            tags = {"Alerts"},
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Alerta encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alerta não encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("{alertId}")
    public ResponseEntity<AlertResponse> getAlertById(
            @Parameter(description = "ID do alerta", required = true)
            @PathVariable UUID alertId) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        Alert alert = alertUseCase.getAlertById(alertId, ownerId);

        AlertResponse response = AlertResponse.builder()
                .id(alert.getId().toString())
                .type(alert.getType().name())
                .timestamp(alert.getTimestamp().toString())
                .severity(alert.getSeverity().name())
                .value(alert.getValue())
                .status(alert.getStatus().name())
                .hiveId(alert.getHive() != null ? alert.getHive().getId().toString() : null)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Buscar alertas por colmeia e status",
            description = "Retorna uma página de alertas filtrados por colmeia e status. Requer autenticação.",
            tags = {"Alerts"},
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Alertas encontrados com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AlertResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/hive/{hiveId}")
    public ResponseEntity<Page<AlertResponse>> getAlertsByHiveIdAndStatus(
            @Parameter(description = "ID da colmeia", required = true)
            @PathVariable UUID hiveId,
            @Parameter(description = "Status do alerta", required = false)
            @RequestParam(required = false) Alert.AlertStatus status,
            Pageable pageable
    ) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        Page<Alert> alerts = alertUseCase.getAlertsByHiveIdAndStatus(hiveId, status, ownerId, pageable);

        Page<AlertResponse> responsePage = alerts.map(alert -> AlertResponse.builder()
                .id(alert.getId().toString())
                .type(alert.getType().name())
                .timestamp(alert.getTimestamp().toString())
                .severity(alert.getSeverity().name())
                .value(alert.getValue())
                .status(alert.getStatus().name())
                .hiveId(alert.getHive() != null ? alert.getHive().getId().toString() : null)
                .build());

        return ResponseEntity.ok(responsePage);
    }

    @Operation(
            summary = "Atualizar status do alerta",
            description = "Atualiza o status de um alerta específico. Requer autenticação.",
            tags = {"Alerts"},
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status do alerta atualizado com sucesso",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alerta não encontrado",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PatchMapping("{alertId}/status/{status}")
    public ResponseEntity<Void> updateAlertStatus(
            @Parameter(description = "ID do alerta", required = true)
            @PathVariable UUID alertId,
            @Parameter(description = "Novo status do alerta", required = true)
            @PathVariable Alert.AlertStatus status) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        alertUseCase.updateAlertStatus(alertId, status, ownerId);

        return ResponseEntity.ok().build();
    }
}
