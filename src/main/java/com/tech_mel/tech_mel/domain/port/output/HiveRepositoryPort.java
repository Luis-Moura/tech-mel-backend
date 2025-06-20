package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Hive;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HiveRepositoryPort {
    Hive save(Hive hive);

    Optional<Hive> findById(UUID hiveId);

    List<Hive> findByOwnerId(UUID ownerId);

    void deleteById(UUID hiveId);

    void updateApiKey(UUID hiveId, String apiKey);

    void updateStatus(UUID hiveId, Hive.HiveStatus newStatus);
}
