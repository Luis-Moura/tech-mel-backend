package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Requisição para reenvio do e-mail de verificação.",
    example = "{\"email\": \"usuario@email.com\"}"
)
public record ResendEmailVerificationRequest(
    @Email
    @NotBlank
    @Schema(
        description = "E-mail do usuário para reenvio da verificação.",
        example = "usuario@email.com"
    )
    String email
) {
}
