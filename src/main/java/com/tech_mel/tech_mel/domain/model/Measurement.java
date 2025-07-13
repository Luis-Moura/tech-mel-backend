package com.tech_mel.tech_mel.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Measurement {
    private UUID id;

    private Double temperature;

    private Double humidity;

    private Double co2;

    private LocalDateTime measuredAt;
}
