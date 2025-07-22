package com.tech_mel.tech_mel.infrastructure.api.dto.response.alert;

import com.tech_mel.tech_mel.domain.model.Alert;
import com.tech_mel.tech_mel.domain.model.Hive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com os dados de um alerta específico")
public class AlertResponse {
    @Schema(description = "ID do alerta", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Tipo do alerta", example = "TEMPERATURE")
    private String type;

    @Schema(description = "Data e hora do alerta", example = "2025-07-13T14:30:00")
    private String timestamp;

    @Schema(description = "Severidade do alerta", example = "HIGH")
    private String severity;

    @Schema(description = "Valor que gerou o alerta", example = "30.5")
    private Double value;

    @Schema(description = "Status do alerta", example = "NEW")
    private String status;

    @Schema(description = "ID da colmeia associada ao alerta", example = "123e4567-e89b-12d3-a456-426614174001")
    private String hiveId;

//    private UUID id;
//    private LocalDateTime timestamp;
//    private Alert.AlertType type; // TEMPERATURE, HUMIDITY, CO2
//    private Alert.AlertSeverity severity; // LOW, MEDIUM, HIGH
//    private Double value; // valor que gerou o alerta
//    private Alert.AlertStatus status; // NEW, VIEWED, RESOLVED
//    private Hive hive;
}
