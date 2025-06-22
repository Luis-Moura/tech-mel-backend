package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.CreateHiveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface HiveUseCase {
    Hive createHive(CreateHiveRequest request);

    Page<Hive> listHivesByOwner(UUID owner, Pageable pageable);

    Optional<Hive> getHiveById(UUID hiveId);

    void updateApiKey(UUID hiveId, String newApiKey);

    void updateHiveStatus(UUID hiveId, Hive.HiveStatus hiveStatus);

    void deleteHive(UUID hiveId);
}
