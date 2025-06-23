package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para renovação de token de acesso")
public record RefreshTokenRequest(
        @Schema(description = "Token de refresh válido", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @NotBlank(message = "O refresh token é obrigatório")
        String refreshToken
) {
}