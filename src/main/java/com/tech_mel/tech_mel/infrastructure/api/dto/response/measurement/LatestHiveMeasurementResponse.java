package com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement;

import com.tech_mel.tech_mel.domain.model.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Resposta contendo a última medição registrada para uma colmeia.",
    example = "{\"hiveId\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\", \"hiveName\": \"Colmeia Principal\", \"latestMeasurement\": {\"id\": \"c47ac10b-58cc-4372-a567-0e02b2c3d479\", \"temperature\": 34.2, \"humidity\": 78.5, \"co2\": 420.0, \"measuredAt\": \"2024-07-15T14:30:00\"}}"
)
public class LatestHiveMeasurementResponse {
    @Schema(description = "ID da colmeia.", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    UUID hiveId;
    @Schema(description = "Nome da colmeia.", example = "Colmeia Principal")
    String hiveName;
    @Schema(description = "Última medição registrada.")
    Measurement latestMeasurement;
}
