package com.tech_mel.tech_mel.infrastructure.api.dto.request.measurement;

import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados das medições dos sensores da colmeia")
public record CreateMeasurementRequest(
        @Schema(
                description = "Temperatura medida em graus Celsius",
                example = "25.5",
                minimum = "-50",
                maximum = "60"
        )
        @NotNull
        Double temperature,

        @Schema(
                description = "Umidade relativa do ar em porcentagem",
                example = "65.0",
                minimum = "0",
                maximum = "100"
        )
        @NotNull
        Double humidity,

        @Schema(
                description = "Concentração de CO2 em ppm (partes por milhão)",
                example = "400.0",
                minimum = "0",
                maximum = "5000"
        )
        @NotNull
        Double co2,

        @Schema(
                description = "Data e hora da medição",
                example = "2025-07-13T14:30:00",
                pattern = "yyyy-MM-ddTHH:mm:ss"
        )
        @NotNull
        LocalDateTime measuredAt
) {
}
