package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.Purchase;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.PurchaseEntity;
import org.springframework.stereotype.Component;

@Component
public class PurchaseMapper {
    private final UserMapper userMapper;

    public PurchaseMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Purchase toDomain(PurchaseEntity entity) {
        if (entity == null) {
            return null;
        }

        return Purchase.builder()
                .id(entity.getId())
                .quantity(entity.getQuantity())
                .amount(entity.getAmount())
                .externalReference(entity.getExternalReference())
                .buyerAddress(entity.getBuyerAddress())
                .buyer(userMapper.toDomain(entity.getBuyer()))
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PurchaseEntity toEntity(Purchase domain) {
        if (domain == null) {
            return null;
        }

        return PurchaseEntity.builder()
                .id(domain.getId())
                .quantity(domain.getQuantity())
                .amount(domain.getAmount())
                .externalReference(domain.getExternalReference())
                .buyerAddress(domain.getBuyerAddress())
                .buyer(userMapper.toEntity(domain.getBuyer()))
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
