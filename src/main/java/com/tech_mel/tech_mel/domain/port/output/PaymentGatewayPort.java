package com.tech_mel.tech_mel.domain.port.output;

import java.util.Map;
import java.util.UUID;

public interface PaymentGatewayPort {
    Map<String, String> createPaymentIntention(int quantity, UUID userId, String buyerAdress);

    void processPaymentNotification(Long paymentId);
}
