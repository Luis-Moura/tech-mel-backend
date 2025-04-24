package com.tech_mel.tech_mel.infrastructure.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        String token,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String newPassword
) {
}
