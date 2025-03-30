package com.tech_mel.tech_mel;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TechMelApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Configurações do banco de dados
        System.setProperty("spring.datasource.url", dotenv.get("DB_URL"));
        System.setProperty("spring.datasource.username", dotenv.get("DB_USERNAME"));
        System.setProperty("spring.datasource.password", dotenv.get("DB_PASSWORD"));

        // Configurações do OAuth2
        System.setProperty("spring.security.oauth2.client.registration.google.client-id", dotenv.get("GOOGLE_CLIENT_ID"));
        System.setProperty("spring.security.oauth2.client.registration.google.client-secret", dotenv.get("GOOGLE_CLIENT_SECRET"));

        // Configurações do JWT
        System.setProperty("jwt.secret", dotenv.get("JWT_SECRET"));
        System.setProperty("jwt.expiration", dotenv.get("JWT_EXPIRATION"));
        System.setProperty("jwt.refresh-expiration", dotenv.get("JWT_REFRESH_EXPIRATION"));

        // Configurações de email
        System.setProperty("spring.mail.host", dotenv.get("MAIL_HOST"));
        System.setProperty("spring.mail.port", dotenv.get("MAIL_PORT"));
        System.setProperty("spring.mail.username", dotenv.get("MAIL_USERNAME"));
        System.setProperty("spring.mail.password", dotenv.get("MAIL_PASSWORD"));
        System.setProperty("spring.mail.from", dotenv.get("MAIL_FROM"));
        System.setProperty("spring.mail.properties.mail.smtp.auth", dotenv.get("MAIL_AUTH"));
        System.setProperty("spring.mail.properties.mail.smtp.starttls.enable", dotenv.get("MAIL_STARTTLS"));

        // Perfil ativo
        System.setProperty("spring.profiles.active", dotenv.get("SPRING_PROFILE"));

        SpringApplication.run(TechMelApplication.class, args);
    }
}