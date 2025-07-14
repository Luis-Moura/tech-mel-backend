package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.DailyMeasurementAverageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DailyMeasurementAverageRepository extends JpaRepository<DailyMeasurementAverageEntity, UUID> {
    Page<DailyMeasurementAverageEntity> findAllByHive_Id(UUID hiveId, Pageable pageable);
}
