package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Requisição para solicitar redefinição de senha.",
    example = "{\"email\": \"usuario@email.com\"}"
)
public record ForgotPasswordRequest(
    @NotBlank
    @Email
    @Schema(
        description = "E-mail do usuário para envio do link de redefinição.",
        example = "usuario@email.com"
    )
    String email
) {
}
