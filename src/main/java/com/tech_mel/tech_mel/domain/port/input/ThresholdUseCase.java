package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.Threshold;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.threshold.CreateThresholdRequest;

import java.util.UUID;

public interface ThresholdUseCase {
    Threshold createThreshold(CreateThresholdRequest request, UUID ownerId);

    Threshold getThresholdById(UUID thresholdId, UUID ownerId);

    Threshold getThresholdByHiveId(UUID hiveId, UUID ownerId);

    void updateThreshold(UUID thresholdId, CreateThresholdRequest request, UUID ownerId);
}
