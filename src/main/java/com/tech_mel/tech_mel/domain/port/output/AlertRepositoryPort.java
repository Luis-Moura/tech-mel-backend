package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AlertRepositoryPort {
    Alert save(Alert alert);

    Optional<Alert> findById(UUID alertId);

    Page<Alert> findAllByHiveIdAndStatus(
            UUID hiveId,
            Alert.AlertStatus status,
            Pageable pageable
    );
}
