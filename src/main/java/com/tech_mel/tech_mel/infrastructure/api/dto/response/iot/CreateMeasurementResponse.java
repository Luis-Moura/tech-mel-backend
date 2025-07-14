package com.tech_mel.tech_mel.infrastructure.api.dto.response.iot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeasurementResponse {
    private Double temperature;

    private Double humidity;

    private Double co2;

}
