package com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Resposta contendo médias diárias das medições de uma colmeia.",
    example = "{\"id\": \"c47ac10b-58cc-4372-a567-0e02b2c3d479\", \"avgTemperature\": 33.5, \"avgHumidity\": 75.2, \"avgCo2\": 410.0, \"date\": \"2024-07-15\", \"hiveId\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\"}"
)
public class DailyMeasurementAveragesResponse {
    @Schema(description = "ID da média diária.", example = "c47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;
    @Schema(description = "Temperatura média do dia (°C).", example = "33.5")
    private double avgTemperature;
    @Schema(description = "Umidade média do dia (%).", example = "75.2")
    private double avgHumidity;
    @Schema(description = "CO2 médio do dia (ppm).", example = "410.0")
    private double avgCo2;
    @Schema(description = "Data da média (formato ISO).", example = "2024-07-15")
    private LocalDate date;
    @Schema(description = "ID da colmeia.", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID hiveId;
}
