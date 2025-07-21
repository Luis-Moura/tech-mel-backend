package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.ThresholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdJpaRepository extends JpaRepository<ThresholdEntity, Long> {
}

