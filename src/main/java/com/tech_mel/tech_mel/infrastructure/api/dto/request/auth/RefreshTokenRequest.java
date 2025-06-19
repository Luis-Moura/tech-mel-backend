package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "O refresh token é obrigatório")
        String refreshToken
) {
}