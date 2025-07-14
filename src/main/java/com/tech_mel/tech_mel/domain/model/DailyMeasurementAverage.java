package com.tech_mel.tech_mel.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DailyMeasurementAverage {
    private UUID id;
    private double avgTemperature;
    private double avgHumidity;
    private double avgCo2;
    private LocalDate date;
    private Hive hive;
}
