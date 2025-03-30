package com.tech_mel.tech_mel.application.port.output;

public interface EmailSenderPort {
    void sendVerificationEmail(String to, String name, String verificationToken);
}