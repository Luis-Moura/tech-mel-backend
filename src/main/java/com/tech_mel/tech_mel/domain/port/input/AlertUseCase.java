package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.Alert;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Measurement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AlertUseCase {
    Alert saveAlert(Measurement measurement, Hive hive, LocalDateTime timestamp);

    Alert getAlertById(UUID alertId, UUID ownerId);

    Page<Alert> getAlertsByHiveIdAndStatus(
            UUID hiveId,
            Alert.AlertStatus status,
            UUID ownerId,
            Pageable pageable
    );

    void updateAlertStatus(UUID alertId, Alert.AlertStatus status, UUID ownerId);
}
