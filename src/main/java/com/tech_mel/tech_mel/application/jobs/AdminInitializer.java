package com.tech_mel.tech_mel.application.jobs;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default.email:admin@techmel.com}")
    private String defaultAdminEmail;

    @Value("${app.admin.default.password:Admin@TechMel2024}")
    private String defaultAdminPassword;

    @Value("${app.admin.default.name:Administrador Principal}")
    private String defaultAdminName;

    @Override
    public void run(ApplicationArguments args) {
        createDefaultAdminIfNotExists();
    }

    private void createDefaultAdminIfNotExists() {
        log.info("Verificando se existe administrador primário no sistema...");

        // Verifica se já existe um admin primário
        List<User> primaryAdmins = userRepositoryPort.findByIsPrimary(true);

        if (!primaryAdmins.isEmpty()) {
            log.info("Administrador primário já existe: {}", primaryAdmins.get(0).getEmail());
            return;
        }

        // Verifica se já existe um usuário com o email padrão
        if (userRepositoryPort.findByEmail(defaultAdminEmail).isPresent()) {
            log.warn("Usuário com email {} já existe, mas não é administrador primário. " +
                    "Considere verificar as configurações.", defaultAdminEmail);
            return;
        }

        log.info("Criando administrador primário padrão...");

        try {
            User defaultAdmin = User.builder()
                    .email(defaultAdminEmail)
                    .name(defaultAdminName)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .enabled(true)
                    .locked(false)
                    .isActive(true)
                    .isPrimary(true) // Administrador primário
                    .requiresPasswordChange(false) // Não força mudança na primeira instalação
                    .authProvider(User.AuthProvider.LOCAL)
                    .availableHives(Integer.MAX_VALUE) // Admin primário tem acesso ilimitado
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User savedAdmin = userRepositoryPort.save(defaultAdmin);

            log.info("✅ Administrador primário criado com sucesso!");
            log.info("📧 Email: {}", savedAdmin.getEmail());
            log.info("🔑 Senha: {} (Altere após o primeiro login)", defaultAdminPassword);
            log.info("🆔 ID: {}", savedAdmin.getId());

            log.warn("⚠️  IMPORTANTE: Altere a senha padrão após o primeiro login por motivos de segurança!");

        } catch (Exception e) {
            log.error("❌ Erro ao criar administrador primário padrão: {}", e.getMessage(), e);
        }
    }
}
