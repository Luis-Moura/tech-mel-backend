package com.tech_mel.tech_mel.domain.port.output;

import java.util.UUID;

public interface EmailSenderPort {
    void sendVerificationEmail(String to, String name, String verificationToken);

    void sendUserDeletionEmail(String to, String name);

    void sendPasswordResetEmail(String to, String name, UUID verificationToken);
}