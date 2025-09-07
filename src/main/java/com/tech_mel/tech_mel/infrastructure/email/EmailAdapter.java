package com.tech_mel.tech_mel.infrastructure.email;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    public void sendPasswordResetEmail(String to, String name, UUID verificationToken) {
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

    @Override
    @Async
    public void sendTechnicianCredentialsEmail(String to, String name, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", to);
            context.setVariable("password", temporaryPassword);
            context.setVariable("loginUrl", appUrlFrontend + "/login");

            String htmlContent = templateEngine.process("/technician-credentials", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("Bem-vindo ao TechMel - Credenciais de Acesso");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de credenciais de técnico enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de credenciais de técnico para: {}", to, e);
            throw new BadRequestException("Erro ao enviar email de credenciais de técnico");
        }
    }

    @Override
    @Async
    public void sendAdminCredentialsEmail(String to, String name, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", to);
            context.setVariable("password", temporaryPassword);
            context.setVariable("loginUrl", appUrlFrontend + "/login");

            String htmlContent = templateEngine.process("email/admin-credentials", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("TechMel - Credenciais de Administrador");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de credenciais de admin enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de credenciais de admin para: {}", to, e);
            throw new BadRequestException("Erro ao enviar email de credenciais de admin");
        }
    }

    @Override
    @Async
    public void sendPasswordResetNotificationEmail(String to, String name, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", to);
            context.setVariable("newPassword", temporaryPassword);
            context.setVariable("loginUrl", appUrlFrontend + "/login");

            String htmlContent = templateEngine.process("email/password-reset-admin", context);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("TechMel - Senha Resetada pelo Administrador");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de notificação de reset de senha enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de notificação de reset de senha para: {}", to, e);
            throw new BadRequestException("Erro ao enviar email de notificação de reset de senha");
        }
    }
}