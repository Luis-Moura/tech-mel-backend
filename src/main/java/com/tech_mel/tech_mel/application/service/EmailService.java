package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import com.tech_mel.tech_mel.domain.event.UserRegisteredEvent;
import com.tech_mel.tech_mel.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailSenderPort emailSenderPort;

    @EventListener
    @Async
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        User user = event.user();
        emailSenderPort.sendVerificationEmail(user.getEmail(), user.getName(), event.verificationToken());
        log.info("Evento de registro de usuário processado para: {}", user.getEmail());
    }
}