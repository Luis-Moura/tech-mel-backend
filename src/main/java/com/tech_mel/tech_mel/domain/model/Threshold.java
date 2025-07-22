package com.tech_mel.tech_mel.domain.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Threshold {
    private UUID id;
    private Double temperatureMin;
    private Double temperatureMax;
    private Double humidityMin;
    private Double humidityMax;
    private Double co2Min;
    private Double co2Max;
    private Hive hive;
}
