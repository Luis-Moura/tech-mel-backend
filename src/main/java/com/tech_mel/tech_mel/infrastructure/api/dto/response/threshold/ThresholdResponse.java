package com.tech_mel.tech_mel.infrastructure.api.dto.response.threshold;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com os dados de criação de um limiar de sensores")
public class ThresholdResponse {
    @Schema(
            description = "ID do limiar criado",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private UUID id;

    @Schema(
            description = "Temperatura mínima permitida em graus Celsius",
            example = "18.0",
            minimum = "-50",
            maximum = "60"
    )
    private Double temperatureMin;

    @Schema(
            description = "Temperatura máxima permitida em graus Celsius",
            example = "30.0",
            minimum = "-50",
            maximum = "60"
    )
    private Double temperatureMax;

    @Schema(
            description = "Umidade mínima permitida em porcentagem",
            example = "30.0",
            minimum = "0",
            maximum = "100"
    )
    private Double humidityMin;

    @Schema(
            description = "Umidade máxima permitida em porcentagem",
            example = "80.0",
            minimum = "0",
            maximum = "100"
    )
    private Double humidityMax;

    @Schema(
            description = "Concentração mínima de CO2 permitida em ppm (partes por milhão)",
            example = "300.0",
            minimum = "0",
            maximum = "5000"
    )
    private Double co2Min;

    @Schema(
            description = "Concentração máxima de CO2 permitida em ppm (partes por milhão)",
            example = "1000.0",
            minimum = "0",
            maximum = "5000"
    )
    private Double co2Max;

    @Schema(
            description = "ID da colmeia associada ao limiar",
            example = "123e4567-e89b-12d3-a456-426614174001"
    )
    private UUID hiveId;
}
