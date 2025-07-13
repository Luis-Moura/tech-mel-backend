package com.tech_mel.tech_mel.infrastructure.api.dto.request.iot;

import org.jetbrains.annotations.NotNull;

public record CreateMeasurementRequest(
        @NotNull
        Double temperature,

        @NotNull
        Double humidity,

        @NotNull
        Double co2
) {
}
