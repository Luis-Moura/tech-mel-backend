package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.AlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertJpaRepository extends JpaRepository<AlertEntity, UUID> {
    Page<AlertEntity> findAllByHiveIdAndStatus(UUID hiveId, AlertEntity.AlertStatus status, Pageable pageable);
}

