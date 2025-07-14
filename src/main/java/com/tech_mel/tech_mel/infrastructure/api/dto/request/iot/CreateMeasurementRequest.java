package com.tech_mel.tech_mel.infrastructure.api.dto.request.iot;

import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public record CreateMeasurementRequest(
        @NotNull
        Double temperature,

        @NotNull
        Double humidity,

        @NotNull
        Double co2,

        @NotNull
        LocalDateTime measuredAt
) {
}
