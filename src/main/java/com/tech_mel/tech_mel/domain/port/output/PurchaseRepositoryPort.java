package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.Purchase;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepositoryPort {
    Purchase save(Purchase purchase);

    Optional<Purchase> findById(UUID id);

    Optional<Purchase> findByExternalReference(String externalReference);
}
