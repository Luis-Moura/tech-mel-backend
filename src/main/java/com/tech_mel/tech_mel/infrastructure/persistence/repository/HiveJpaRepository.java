package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.HiveEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HiveJpaRepository extends JpaRepository<HiveEntity, UUID> {
    Page<HiveEntity> findByOwner_Id(UUID ownerId, Pageable pageable);

    Optional<HiveEntity> findByApiKey(String apiKey);
}
