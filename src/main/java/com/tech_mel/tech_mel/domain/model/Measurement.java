package com.tech_mel.tech_mel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Measurement {
    private UUID id;

    private Double humidity;

    private Double co2;

    private LocalDateTime measuredAt;
}
