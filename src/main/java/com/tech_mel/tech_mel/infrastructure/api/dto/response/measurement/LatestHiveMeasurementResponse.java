package com.tech_mel.tech_mel.infrastructure.api.dto.response.measurement;

import com.tech_mel.tech_mel.domain.model.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestHiveMeasurementResponse {
    UUID hiveId;
    String hiveName;
    Measurement latestMeasurement;
}
