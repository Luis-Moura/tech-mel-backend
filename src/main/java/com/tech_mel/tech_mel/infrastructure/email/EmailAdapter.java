package com.tech_mel.tech_mel.infrastructure.email;

import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.url}")
    private String appUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String to, String name, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verificationUrl",
                    appUrl + "/api/auth/verify?token=" + verificationToken);

            String htmlContent = templateEngine.process("/email-verification", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Verificação de Email - Tech Mel");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de verificação enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de verificação para: {}", to, e);
            throw new RuntimeException("Erro ao enviar email de verificação", e);
        }
    }
}