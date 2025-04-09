package com.tech_mel.tech_mel.infrastructure.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailVerificationRequest(
        @Email
        @NotBlank
        String email
) {
}
