package com.tech_mel.tech_mel.infrastructure.api.dto.request.purchase;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentIntentionRequest(
        int quantity,

        @NotBlank
        String buyerAdress) {
}
