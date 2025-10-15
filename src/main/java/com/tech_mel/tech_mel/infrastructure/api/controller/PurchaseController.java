package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.port.output.PaymentGatewayPort;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.purchase.CreatePaymentIntentionRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.purchase.PaymentIntentionResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
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

    @Value("${mercadopago.webhook.key}")
    private String webhookKey;

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

    @PostMapping("/webhook")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestBody Map<String, Object> payload
//            @RequestHeader(value = "x-signature", required = false) String signatureHeader
    ) {
        try {
//            if (signatureHeader == null) {
//                return ResponseEntity.status(403).build();
//            }
            System.out.println(payload);

            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            Long paymentId = Long.parseLong(data.get("id").toString());
            String type = payload.get("type").toString();

//            if (!isSignatureValid(paymentId, type, signatureHeader)) {
//                return ResponseEntity.status(403).build();
//            }

            if (!"payment".equalsIgnoreCase(type)) {
                return ResponseEntity.ok().build(); // ignora outros tipos
            }

            paymentGatewayPort.processPaymentNotification(paymentId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean isSignatureValid(Long id, String type, String receivedSignature) {
        try {
            String message = id + ":" + type + ":" + webhookKey;

            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKey);

            String calculatedSignature = Base64.getEncoder()
                    .encodeToString(sha256_HMAC.doFinal(message.getBytes()));

            return calculatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
