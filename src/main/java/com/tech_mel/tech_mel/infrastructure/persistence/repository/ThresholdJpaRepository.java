package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.ThresholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ThresholdJpaRepository extends JpaRepository<ThresholdEntity, UUID> {
    Optional<ThresholdEntity> findByHiveId(UUID hiveId);
}

