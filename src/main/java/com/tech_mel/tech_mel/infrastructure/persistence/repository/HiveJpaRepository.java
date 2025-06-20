package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.HiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HiveJpaRepository extends JpaRepository<HiveEntity, UUID> {
    List<HiveEntity> findByOwner_Id(UUID ownerId);
}
