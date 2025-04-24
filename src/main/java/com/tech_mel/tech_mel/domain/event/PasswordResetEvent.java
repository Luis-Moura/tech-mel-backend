package com.tech_mel.tech_mel.domain.event;

import java.util.UUID;

public record PasswordResetEvent(
        String email,
        String name,
        UUID passwordResetToken
) {
}
