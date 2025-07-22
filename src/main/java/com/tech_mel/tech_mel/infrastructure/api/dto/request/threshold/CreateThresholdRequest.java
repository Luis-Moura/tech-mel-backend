package com.tech_mel.tech_mel.infrastructure.api.dto.request.threshold;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para criação de limites de sensores da colmeia")
public record CreateThresholdRequest(
        @Schema(
                description = "Temperatura mínima permitida em graus Celsius",
                example = "10.0",
                minimum = "-50",
                maximum = "60"
        )
        @NotNull
        Double temperatureMin,

        @Schema(
                description = "Temperatura máxima permitida em graus Celsius",
                example = "40.0",
                minimum = "-50",
                maximum = "60"
        )
        @NotNull
        Double temperatureMax,

        @Schema(
                description = "Umidade mínima permitida em porcentagem",
                example = "30.0",
                minimum = "0",
                maximum = "100"
        )
        @NotNull
        Double humidityMin,

        @Schema(
                description = "Umidade máxima permitida em porcentagem",
                example = "80.0",
                minimum = "0",
                maximum = "100"
        )
        @NotNull
        Double humidityMax,

        @Schema(
                description = "CO2 mínimo permitido em ppm (partes por milhão)",
                example = "350.0",
                minimum = "0",
                maximum = "5000"
        )
        @NotNull
        Double co2Min,

        @Schema(
                description = "CO2 máximo permitido em ppm (partes por milhão)",
                example = "1200.0",
                minimum = "0",
                maximum = "5000"
        )
        @NotNull
        Double co2Max,

        @Schema(
                description = "Identificador da colmeia",
                example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
        )
        @NotNull
        UUID hiveId
) {
}
