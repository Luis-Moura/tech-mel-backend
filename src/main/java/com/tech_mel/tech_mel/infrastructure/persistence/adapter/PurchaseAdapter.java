package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.Purchase;
import com.tech_mel.tech_mel.domain.port.output.PurchaseRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.PurchaseEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.PurchaseMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.PurchaseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PurchaseAdapter implements PurchaseRepositoryPort {
    private final PurchaseJpaRepository repository;
    private final PurchaseMapper purchaseMapper;

    @Override
    public Purchase save(Purchase purchase) {
        PurchaseEntity entity = purchaseMapper.toEntity(purchase);
        PurchaseEntity savedEntity = repository.save(entity);
        return purchaseMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return repository.findById(id)
                .map(purchaseMapper::toDomain);
    }

    @Override
    public Optional<Purchase> findByExternalReference(String externalReference) {
        return repository.findByExternalReference(externalReference)
                .map(purchaseMapper::toDomain);
    }
}
