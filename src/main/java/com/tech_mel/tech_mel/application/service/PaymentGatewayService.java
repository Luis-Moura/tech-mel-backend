package com.tech_mel.tech_mel.application.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.domain.model.Purchase;
import com.tech_mel.tech_mel.domain.model.PurchaseStatus;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.output.PaymentGatewayPort;
import com.tech_mel.tech_mel.domain.port.output.PurchaseRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService implements PaymentGatewayPort {
    private final UserService userService;

    private final PurchaseRepositoryPort purchaseRepositoryPort;

    private final UserRepositoryPort userRepositoryPort;

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${app.url.frontend}")
    private String frontendUrl;

    @Value("${app.url}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        System.out.println("✅ Token do Mercado Pago configurado com sucesso!");
    }

    @Override
    public Map<String, String> createPaymentIntention(int quantity, UUID userId, String buyerAdress) {
        User user = userService.getCurrentUser(userId);

        String externalReference = "order-" + UUID.randomUUID();
        BigDecimal unitPrice = BigDecimal.valueOf(149.99);
        int unitPriceInCents = unitPrice.multiply(BigDecimal.valueOf(100)).intValueExact();

        if (quantity > 10) {
            unitPrice = unitPrice.multiply(BigDecimal.valueOf(0.9)); // 10% de desconto
        }

        PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                .id("placa-iot-01")
                .title("Aparelho IoT - Tech Mel")
                .description("Aparelho IoT para monitoramento e controle de caixas de abelha")
                .categoryId("electronics")
                .currencyId("BRL")
                .quantity(quantity)
                .unitPrice(unitPrice)
                .build();

        List<PreferenceItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(items)
//                .backUrls(
//                        PreferenceBackUrlsRequest.builder()
//                                .success(frontendUrl + "/payment/success")
//                                .failure(frontendUrl + "/payment/failure")
//                                .pending(frontendUrl + "/payment/pending")
//                                .build()
//                )
//                .autoReturn("approved")
//                .payer(
//                        PreferencePayerRequest.builder()
//                                .name(user.getName())
//                                .email(user.getEmail()).build()
//                )
                .notificationUrl(baseUrl + "/api/purchases/webhook")
                .externalReference(externalReference)
                .build();

        PreferenceClient client = new PreferenceClient();

        Preference preference;

        try {
            preference = client.create(preferenceRequest);
        } catch (MPException e) {
            log.error("Erro genérico do MercadoPago: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar intenção de pagamento", e);
        } catch (MPApiException e) {
            log.error("Erro da API do MercadoPago: {}", e.getMessage());
            log.error("Status code: {}", e.getApiResponse().getStatusCode());
            log.error("Response content: {}", e.getApiResponse().getContent());
            throw new RuntimeException("Erro ao criar intenção de pagamento", e);
        }

        Purchase purchase = Purchase.builder()
                .externalReference(externalReference)
                .buyer(user)
                .quantity(quantity)
                .amount(unitPriceInCents)
                .status(PurchaseStatus.PENDING)
                .buyerAddress(buyerAdress)
                .build();

        purchaseRepositoryPort.save(purchase);

        log.info("Intenção de pagamento criada com sucesso para o usuário {}: {}", user.getEmail(), preference.getId());

        return Map.of(
                "initPoint", preference.getInitPoint(),
                "externalReference", externalReference
        );
    }

    @Override
    public void processPaymentNotification(Long paymentId) {

        try {
            var paymentClient = new PaymentClient();

            var payment = paymentClient.get(paymentId);

            String status = payment.getStatus();
            String externalReference = payment.getExternalReference();

            log.info("📦 Notificação recebida - PaymentID={} | Status={} | Reference={}", paymentId, status, externalReference);

            Purchase purchase = purchaseRepositoryPort.findByExternalReference(externalReference)
                    .orElseThrow(() -> new BadRequestException("Compra não encontrada para a referência: " + externalReference));

            switch (status) {
                case "approved" -> purchase.setStatus(PurchaseStatus.PAID);
                case "rejected" -> purchase.setStatus(PurchaseStatus.FAILED);
                default -> purchase.setStatus(PurchaseStatus.PENDING);
            }

            purchaseRepositoryPort.save(purchase);

            log.info("✅ Pedido atualizado com sucesso: {} -> {}", externalReference, purchase.getStatus());

            if (purchase.getStatus() == PurchaseStatus.PAID) {
                User buyer = purchase.getBuyer();

                int incrementsBuyerAvailableHives = buyer.getAvailableHives() + purchase.getQuantity();

                buyer.setAvailableHives(incrementsBuyerAvailableHives);

                userRepositoryPort.save(buyer);
            }

        } catch (MPApiException e) {
            log.error("Erro na API do Mercado Pago: {}", e.getMessage());
            log.error("Detalhes: {}", e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("Erro geral do SDK Mercado Pago: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar notificação: {}", e.getMessage(), e);
        }
    }
}
