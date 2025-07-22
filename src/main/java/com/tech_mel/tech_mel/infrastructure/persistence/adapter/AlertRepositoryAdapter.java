package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.Alert;
import com.tech_mel.tech_mel.domain.port.output.AlertRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.AlertEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.AlertMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.AlertJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertRepositoryAdapter implements AlertRepositoryPort {
    private final AlertMapper alertMapper;
    private final AlertJpaRepository repository;

    @Override
    public Alert save(Alert alert) {
        AlertEntity alertEntity = alertMapper.toEntity(alert);

        AlertEntity savedEntity = repository.save(alertEntity);

        return alertMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Alert> findById(UUID alertId) {
        return repository.findById(alertId)
                .map(alertMapper::toDomain);
    }

    @Override
    public Page<Alert> findAllByHiveId(UUID hiveId, Pageable pageable) {
        Page<AlertEntity> alertEntityPage = repository.findAllByHiveId(hiveId, pageable);
        return alertEntityPage.map(alertMapper::toDomain);
    }

    @Override
    public Page<Alert> findAllByHiveIdAndStatus(
            UUID hiveId,
            Alert.AlertStatus status,
            Pageable pageable
    ) {
        AlertEntity.AlertStatus alertStatus = AlertEntity.AlertStatus.valueOf(status.name());

        Page<AlertEntity> alertEntityPage = repository
                .findAllByHiveIdAndStatus(hiveId, alertStatus, pageable);

        return alertEntityPage.map(alertMapper::toDomain);
    }
}
