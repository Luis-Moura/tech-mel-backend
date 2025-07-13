package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Hive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface HiveRepositoryPort {
    Hive save(Hive hive);

    Optional<Hive> findById(UUID hiveId);

    Optional<Hive> findByApiKey(String apiKey);

    Page<Hive> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Hive> findAllHives(Pageable pageable);

    void deleteById(UUID hiveId);
}
