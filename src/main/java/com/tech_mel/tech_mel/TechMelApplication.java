package com.tech_mel.tech_mel;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Objects;

@SpringBootApplication
public class TechMelApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Configurações do banco de dados
        System.setProperty("spring.datasource.url", Objects.requireNonNull(dotenv.get("DB_URL")));
        System.setProperty("spring.datasource.username", Objects.requireNonNull(dotenv.get("DB_USERNAME")));
        System.setProperty("spring.datasource.password", Objects.requireNonNull(dotenv.get("DB_PASSWORD")));

        // Configuração do redis
        System.setProperty("spring.data.redis.host", Objects.requireNonNull(dotenv.get("REDIS_HOST")));
        System.setProperty("spring.data.redis.port", Objects.requireNonNull(dotenv.get("REDIS_PORT")));
        System.setProperty("spring.data.redis.password", Objects.requireNonNull(dotenv.get("REDIS_PASSWORD")));
        System.setProperty("spring.data.redis.timeout", Objects.requireNonNull(dotenv.get("REDIS_TIMEOUT")));
        System.setProperty("spring.data.redis.lettuce.pool.max-active", Objects.requireNonNull(dotenv.get("REDIS_POOL_MAX_TOTAL")));
        System.setProperty("spring.data.redis.lettuce.pool.max-idle", Objects.requireNonNull(dotenv.get("REDIS_POOL_MAX_IDLE")));
        System.setProperty("spring.data.redis.lettuce.pool.min-idle", Objects.requireNonNull(dotenv.get("REDIS_POOL_MIN_IDLE")));

        // Configurações do OAuth2
        System.setProperty("app.oauth2.redirect-uri", Objects.requireNonNull(dotenv.get("REDIRECT_URI")));
        System.setProperty("spring.security.oauth2.client.registration.google.redirect-uri", Objects.requireNonNull(dotenv.get("REDIRECT_URI")));
        System.setProperty("spring.security.oauth2.client.registration.google.client-id", Objects.requireNonNull(dotenv.get("GOOGLE_CLIENT_ID")));
        System.setProperty("spring.security.oauth2.client.registration.google.client-secret", Objects.requireNonNull(dotenv.get("GOOGLE_CLIENT_SECRET")));

        // Configurações do JWT
        System.setProperty("jwt.secret", Objects.requireNonNull(dotenv.get("JWT_SECRET")));
        System.setProperty("jwt.expiration", Objects.requireNonNull(dotenv.get("JWT_EXPIRATION")));
        System.setProperty("jwt.refresh-expiration", Objects.requireNonNull(dotenv.get("JWT_REFRESH_EXPIRATION")));

        // Configurações de email
        System.setProperty("spring.mail.host", Objects.requireNonNull(dotenv.get("MAIL_HOST")));
        System.setProperty("spring.mail.port", Objects.requireNonNull(dotenv.get("MAIL_PORT")));
        System.setProperty("spring.mail.username", Objects.requireNonNull(dotenv.get("MAIL_USERNAME")));
        System.setProperty("spring.mail.password", Objects.requireNonNull(dotenv.get("MAIL_PASSWORD")));
        System.setProperty("spring.mail.from", Objects.requireNonNull(dotenv.get("MAIL_FROM")));
        System.setProperty("spring.mail.properties.mail.smtp.auth", Objects.requireNonNull(dotenv.get("MAIL_AUTH")));
        System.setProperty("spring.mail.properties.mail.smtp.starttls.enable", Objects.requireNonNull(dotenv.get("MAIL_STARTTLS")));

        // Perfil ativo
        System.setProperty("spring.profiles.active", Objects.requireNonNull(dotenv.get("SPRING_PROFILE")));

        SpringApplication.run(TechMelApplication.class, args);
    }
}