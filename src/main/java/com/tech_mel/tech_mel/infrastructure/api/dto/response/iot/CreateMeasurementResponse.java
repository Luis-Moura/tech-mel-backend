package com.tech_mel.tech_mel.infrastructure.api.dto.response.iot;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com os dados da medição registrada")
public class CreateMeasurementResponse {
    @Schema(
            description = "Temperatura registrada em graus Celsius",
            example = "25.5"
    )
    private Double temperature;

    @Schema(
            description = "Umidade relativa registrada em porcentagem",
            example = "65.0"
    )
    private Double humidity;

    @Schema(
            description = "Concentração de CO2 registrada em ppm",
            example = "400.0"
    )
    private Double co2;

}
