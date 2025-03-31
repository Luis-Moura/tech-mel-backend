package com.tech_mel.tech_mel.domain.port.output;

public interface EmailSenderPort {
    void sendVerificationEmail(String to, String name, String verificationToken);
}