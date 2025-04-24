package com.tech_mel.tech_mel.domain.event;

public record PasswordResetEvent(
        String email,
        String name,
        String passwordResetToken
) {
}
