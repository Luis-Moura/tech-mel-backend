package com.tech_mel.tech_mel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Purchase {
    private UUID id;

    private int quantity;

    private int amount;

    private String externalReference;

    private String buyerAddress;

    private PurchaseStatus status;

    private User buyer;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
