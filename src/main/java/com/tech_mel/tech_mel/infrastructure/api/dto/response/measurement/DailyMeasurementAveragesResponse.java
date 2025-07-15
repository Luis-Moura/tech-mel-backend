package com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMeasurementAveragesResponse {
    private UUID id;
    private double avgTemperature;
    private double avgHumidity;
    private double avgCo2;
    private LocalDate date;
    private UUID hiveId;
}
