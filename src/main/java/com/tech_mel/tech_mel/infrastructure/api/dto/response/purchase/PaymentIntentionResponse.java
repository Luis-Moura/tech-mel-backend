package com.tech_mel.tech_mel.infrastructure.api.dto.response.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentionResponse {
    String initPoint;

    String externalReference;
}
