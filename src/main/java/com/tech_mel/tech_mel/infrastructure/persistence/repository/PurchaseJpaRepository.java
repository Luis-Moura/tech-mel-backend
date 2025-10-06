package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseJpaRepository extends JpaRepository<PurchaseEntity, UUID> {
    Optional<PurchaseEntity> findByExternalReference(String externalReference);
}
