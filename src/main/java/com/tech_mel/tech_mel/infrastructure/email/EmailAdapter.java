package com.tech_mel.tech_mel.infrastructure.email;

import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.url.frontend}")
    private String appUrlFrontend;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    @Async
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
            throw new BadRequestException("Erro ao enviar email de verificação");
        }
    }

    @Override
    @Async
    public void sendUserDeletionEmail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);

            String htmlContent = templateEngine.process("/email-deletion", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Sua conta foi desativada - Tech Mel");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de deleção enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de deleção para: {}", to, e);
            throw new BadRequestException("Erro ao enviar email de deleção");
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String name, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetUrl",
                    appUrlFrontend + "/api/auth/reset-password?token=" + verificationToken);

            String htmlContent = templateEngine.process("/email-reset-password", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Redefinição de Senha - Tech Mel");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de redefinição de senha enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de redefinição de senha para: {}", to, e);
            throw new BadRequestException("Erro ao enviar email de redefinição de senha");
        }
    }
}