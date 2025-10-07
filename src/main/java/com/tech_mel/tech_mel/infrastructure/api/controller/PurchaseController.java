package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.port.output.PaymentGatewayPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.purchase.CreatePaymentIntentionRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.purchase.PaymentIntentionResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@Tag(name = "purchases", description = "Operações de compra do sistema")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseController {
    private final PaymentGatewayPort paymentGatewayPort;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/payment-intention")
    public ResponseEntity<PaymentIntentionResponse> createPaymentIntention(@Valid @RequestBody CreatePaymentIntentionRequest request) {
        UUID userId = authenticationUtil.getCurrentUserId();

        Map<String, String> intention = paymentGatewayPort.createPaymentIntention(
                request.quantity(),
                userId,
                request.buyerAdress()
        );

        PaymentIntentionResponse response = new PaymentIntentionResponse(
                intention.get("initPoint"),
                intention.get("externalReference")
        );

        return ResponseEntity.ok(response);
    }
}
