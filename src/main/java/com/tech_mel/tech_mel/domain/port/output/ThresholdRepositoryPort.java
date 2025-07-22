package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Threshold;

import java.util.Optional;
import java.util.UUID;

public interface ThresholdRepositoryPort {
    Threshold save(Threshold threshold);

    Optional<Threshold> findById(UUID thresholdId);

    Optional<Threshold> findByHiveId(UUID hiveId);

    void update(Threshold threshold);
}
