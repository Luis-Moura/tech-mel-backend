package com.tech_mel.tech_mel.infrastructure.api.dto.request.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String oldPassword,

        @NotBlank
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String newPassword
) {
}
