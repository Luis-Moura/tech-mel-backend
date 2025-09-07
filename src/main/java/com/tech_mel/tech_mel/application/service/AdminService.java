package com.tech_mel.tech_mel.application.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tech_mel.tech_mel.application.exception.BadRequestException;
import com.tech_mel.tech_mel.application.exception.ConflictException;
import com.tech_mel.tech_mel.application.exception.ForbiddenException;
import com.tech_mel.tech_mel.application.exception.NotFoundException;
import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.EntityType;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AdminUseCase;
import com.tech_mel.tech_mel.domain.port.input.AuditUseCase;
import com.tech_mel.tech_mel.domain.port.output.EmailSenderPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService implements AdminUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final EmailSenderPort emailSenderPort;
    private final AuditUseCase auditUseCase;
    private final AuthenticationUtil authenticationUtil;

    private static final String TEMP_PASSWORD_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    @Override
    public User createTechnician(String email, String name, String password) {
        log.info("Criando novo técnico: {}", email);
        
        // Verifica se o email já existe
        if (userRepositoryPort.findByEmail(email).isPresent()) {
            throw new ConflictException("Email já está em uso");
        }

        // Gera senha temporária se não fornecida
        String finalPassword = password != null ? password : generateTemporaryPassword();
        
        User technician = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(finalPassword))
                .role(User.Role.TECHNICIAN)
                .emailVerified(true) // Técnicos são pré-verificados
                .enabled(true)
                .locked(false)
                .isActive(true)
                .isPrimary(false)
                .requiresPasswordChange(password == null) // Se senha foi gerada, requer mudança
                .authProvider(User.AuthProvider.LOCAL)
                .availableHives(10) // Padrão para técnicos
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedTechnician = userRepositoryPort.save(technician);
        
        // Envia email com credenciais se senha foi gerada
        if (password == null) {
            sendTechnicianCredentialsEmail(savedTechnician, finalPassword);
        }
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.CREATE, 
            EntityType.USER, 
            savedTechnician.getId().toString(),
            "Técnico criado: " + savedTechnician.getEmail()
        );
        
        log.info("Técnico criado com sucesso: {}", savedTechnician.getId());
        return savedTechnician;
    }

    @Override
    public User updateTechnician(UUID technicianId, String name, String email, Integer availableHives) {
        log.info("Atualizando técnico: {}", technicianId);
        
        User technician = userRepositoryPort.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("Técnico não encontrado"));
        
        if (technician.getRole() != User.Role.TECHNICIAN) {
            throw new BadRequestException("Usuário não é um técnico");
        }
        
        // Verifica se o email já está em uso por outro usuário
        if (email != null && !email.equals(technician.getEmail()) && 
            userRepositoryPort.existsByEmailAndIdNot(email, technicianId)) {
            throw new ConflictException("Email já está em uso");
        }
        
        String oldValues = buildUserString(technician);
        
        // Atualiza os campos
        if (name != null) technician.setName(name);
        if (email != null) technician.setEmail(email);
        if (availableHives != null) technician.setAvailableHives(availableHives);
        technician.setUpdatedAt(LocalDateTime.now());
        
        User updatedTechnician = userRepositoryPort.save(technician);
        String newValues = buildUserString(updatedTechnician);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.UPDATE, 
            EntityType.USER, 
            updatedTechnician.getId().toString(),
            "Técnico atualizado",
            oldValues,
            newValues,
            null,
            null
        );
        
        log.info("Técnico atualizado com sucesso: {}", updatedTechnician.getId());
        return updatedTechnician;
    }

    @Override
    public void deleteTechnician(UUID technicianId) {
        log.info("Deletando técnico: {}", technicianId);
        
        User technician = userRepositoryPort.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("Técnico não encontrado"));
        
        if (technician.getRole() != User.Role.TECHNICIAN) {
            throw new BadRequestException("Usuário não é um técnico");
        }
        
        userRepositoryPort.deleteById(technicianId);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.DELETE, 
            EntityType.USER, 
            technicianId.toString(),
            "Técnico deletado: " + technician.getEmail()
        );
        
        log.info("Técnico deletado com sucesso: {}", technicianId);
    }

    @Override
    public List<User> getAllTechnicians() {
        return userRepositoryPort.findByRole(User.Role.TECHNICIAN);
    }

    @Override
    public User createSecondaryAdmin(String email, String name, String password) {
        log.info("Criando administrador secundário: {}", email);
        
        // Validar se o usuário atual é admin primário
        UUID currentAdminId = getCurrentAdminId();
        if (currentAdminId == null || !canManageAdmins(currentAdminId)) {
            throw new ForbiddenException("Apenas administradores primários podem criar outros administradores");
        }
        
        // Verificar se email já existe
        if (userRepositoryPort.existsByEmail(email)) {
            throw new ConflictException("Email já cadastrado");
        }

        // Gera senha temporária se não fornecida
        String finalPassword = password != null ? password : generateTemporaryPassword();
        
        User admin = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(finalPassword))
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .enabled(true)
                .locked(false)
                .isActive(true)
                .isPrimary(false) // Sempre false para admins secundários
                .requiresPasswordChange(password == null)
                .authProvider(User.AuthProvider.LOCAL)
                .availableHives(50) // Padrão para admins
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedAdmin = userRepositoryPort.save(admin);
        
        // Envia email com credenciais se senha foi gerada
        if (password == null) {
            sendAdminCredentialsEmail(savedAdmin, finalPassword);
        }
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.CREATE, 
            EntityType.USER, 
            savedAdmin.getId().toString(),
            "Administrador secundário criado: " + savedAdmin.getEmail()
        );
        
        log.info("Administrador secundário criado com sucesso: {}", savedAdmin.getId());
        return savedAdmin;
    }

    @Override
    public List<User> getAllAdmins() {
        return userRepositoryPort.findByRole(User.Role.ADMIN);
    }

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepositoryPort.findAll(pageable);
    }

    @Override
    public Page<User> getUsersByRole(User.Role role, Pageable pageable) {
        return userRepositoryPort.findByRole(role, pageable);
    }

    @Override
    public Page<User> getUsersByStatus(boolean isActive, Pageable pageable) {
        return userRepositoryPort.findByIsActive(isActive, pageable);
    }

    @Override
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepositoryPort.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(
            searchTerm, searchTerm, pageable
        );
    }

    @Override
    public void activateUser(UUID userId) {
        log.info("Ativando usuário: {}", userId);
        
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        
        if (user.isActive()) {
            throw new BadRequestException("Usuário já está ativo");
        }
        
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.ACTIVATE, 
            EntityType.USER, 
            userId.toString(),
            "Usuário ativado: " + user.getEmail()
        );
        
        log.info("Usuário ativado com sucesso: {}", userId);
    }

    @Override
    public void deactivateUser(UUID userId) {
        log.info("Desativando usuário: {}", userId);
        
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        
        // Não permite desativar admin primário
        if (user.getRole() == User.Role.ADMIN && user.isPrimary()) {
            throw new ForbiddenException("Não é possível desativar o administrador primário");
        }
        
        if (!user.isActive()) {
            throw new BadRequestException("Usuário já está inativo");
        }
        
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.DEACTIVATE, 
            EntityType.USER, 
            userId.toString(),
            "Usuário desativado: " + user.getEmail()
        );
        
        log.info("Usuário desativado com sucesso: {}", userId);
    }

    @Override
    public void resetUserPassword(UUID userId) {
        log.info("Resetando senha do usuário: {}", userId);
        
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        // Verifica se o usuário a ser resetado é um administrador primário
        if (user.getRole() == User.Role.ADMIN && user.isPrimary()) {
            throw new ForbiddenException("Não é possível mudar a senha do administrador primário");
        }
        
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setRequiresPasswordChange(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);
        
        // Envia email com nova senha temporária
        sendPasswordResetEmail(user, temporaryPassword);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.PASSWORD_RESET, 
            EntityType.USER, 
            userId.toString(),
            "Senha resetada para: " + user.getEmail()
        );
        
        log.info("Senha resetada com sucesso para usuário: {}", userId);
    }

    @Override
    public void forcePasswordChange(UUID userId) {
        log.info("Forçando mudança de senha para usuário: {}", userId);
        
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        // Verifica se o usuário a ser resetado é um administrador primário
        if (user.getRole() == User.Role.ADMIN && user.isPrimary()) {
            throw new ForbiddenException("Não é possível mudar a senha do administrador primário");
        }

        user.setRequiresPasswordChange(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.PASSWORD_CHANGE, 
            EntityType.USER, 
            userId.toString(),
            "Mudança de senha forçada para: " + user.getEmail()
        );
        
        log.info("Mudança de senha forçada para usuário: {}", userId);
    }

    @Override
    public void unlockUser(UUID userId) {
        log.info("Desbloqueando usuário: {}", userId);
        
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        
        user.setLocked(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.ACCOUNT_UNLOCKED, 
            EntityType.USER, 
            userId.toString(),
            "Usuário desbloqueado: " + user.getEmail()
        );
        
        log.info("Usuário desbloqueado com sucesso: {}", userId);
    }

    @Override
    public void lockUser(UUID userId) {
        log.info("Bloqueando usuário: {}", userId);
        
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        
        // Não permite bloquear admin primário
        if (user.getRole() == User.Role.ADMIN && user.isPrimary()) {
            throw new ForbiddenException("Não é possível bloquear o administrador primário");
        }
        
        user.setLocked(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepositoryPort.save(user);
        
        // Log de auditoria
        auditUseCase.logAction(
            getCurrentAdminId(), 
            AuditAction.ACCOUNT_LOCKED, 
            EntityType.USER, 
            userId.toString(),
            "Usuário bloqueado: " + user.getEmail()
        );
        
        log.info("Usuário bloqueado com sucesso: {}", userId);
    }

    @Override
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Estatísticas de usuários
        long totalUsers = userRepositoryPort.countByIsActive(true) + userRepositoryPort.countByIsActive(false);
        long activeUsers = userRepositoryPort.countByIsActive(true);
        long inactiveUsers = userRepositoryPort.countByIsActive(false);
        
        // Estatísticas por role
        long admins = userRepositoryPort.countByRole(User.Role.ADMIN);
        long technicians = userRepositoryPort.countByRole(User.Role.TECHNICIAN);
        long commonUsers = userRepositoryPort.countByRole(User.Role.COMMON);
        
        // Estatísticas temporais
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
        long newUsersLastMonth = userRepositoryPort.countByCreatedAtAfter(lastMonth);
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", inactiveUsers);
        stats.put("admins", admins);
        stats.put("technicians", technicians);
        stats.put("commonUsers", commonUsers);
        stats.put("newUsersLastMonth", newUsersLastMonth);
        stats.put("generatedAt", LocalDateTime.now());
        
        return stats;
    }

    @Override
    public Map<String, Long> getUserStatisticsByRole() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("ADMIN", userRepositoryPort.countByRole(User.Role.ADMIN));
        stats.put("TECHNICIAN", userRepositoryPort.countByRole(User.Role.TECHNICIAN));
        stats.put("COMMON", userRepositoryPort.countByRole(User.Role.COMMON));
        return stats;
    }

    @Override
    public Map<String, Long> getActiveUsersStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("active", userRepositoryPort.countByIsActive(true));
        stats.put("inactive", userRepositoryPort.countByIsActive(false));
        return stats;
    }

    @Override
    public List<User> getRecentlyRegisteredUsers(int limit) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        return userRepositoryPort.findByCreatedAtAfter(cutoff);
    }

    @Override
    public List<User> getInactiveUsers(int daysInactive) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysInactive);
        return userRepositoryPort.findByLastLoginBefore(cutoff);
    }

    @Override
    public boolean isPrimaryAdmin(UUID userId) {
        return userRepositoryPort.findById(userId)
                .map(user -> user.getRole() == User.Role.ADMIN && user.isPrimary())
                .orElse(false);
    }

    @Override
    public boolean canManageAdmins(UUID currentUserId) {
        return isPrimaryAdmin(currentUserId);
    }

    @Override
    public boolean canManageUser(UUID currentUserId, UUID targetUserId) {
        User currentUser = userRepositoryPort.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Usuário atual não encontrado"));
        
        if (currentUser.getRole() != User.Role.ADMIN) {
            return false;
        }
        
        // Admin primário pode gerenciar qualquer usuário
        if (currentUser.isPrimary()) {
            return true;
        }
        
        // Admin secundário não pode gerenciar outros admins
        User targetUser = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuário alvo não encontrado"));
        
        return targetUser.getRole() != User.Role.ADMIN;
    }

    // Métodos auxiliares privados
    
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(TEMP_PASSWORD_CHARACTERS.charAt(random.nextInt(TEMP_PASSWORD_CHARACTERS.length())));
        }
        
        return password.toString();
    }
    
    private void sendTechnicianCredentialsEmail(User technician, String password) {
        try {
            emailSenderPort.sendTechnicianCredentialsEmail(
                technician.getEmail(), 
                technician.getName(), 
                password
            );
            log.info("Email de credenciais enviado para técnico: {}", technician.getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email de credenciais para técnico: {}", e.getMessage(), e);
        }
    }

    private void sendAdminCredentialsEmail(User admin, String password) {
        try {
            emailSenderPort.sendAdminCredentialsEmail(
                admin.getEmail(), 
                admin.getName(), 
                password
            );
            log.info("Email de credenciais enviado para admin: {}", admin.getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email de credenciais para admin: {}", e.getMessage(), e);
        }
    }

    private void sendPasswordResetEmail(User user, String temporaryPassword) {
        try {
            emailSenderPort.sendPasswordResetNotificationEmail(
                user.getEmail(), 
                user.getName(), 
                temporaryPassword
            );
            log.info("Email de reset de senha enviado para: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erro ao enviar email de reset de senha: {}", e.getMessage(), e);
        }
    }
    
    private String buildUserString(User user) {
        return String.format("ID: %s, Email: %s, Nome: %s, Role: %s, Ativo: %s", 
            user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isActive());
    }
    
    private UUID getCurrentAdminId() {
        try {
            return authenticationUtil.getCurrentUserId();
        } catch (Exception e) {
            log.warn("Não foi possível obter o ID do admin atual: {}", e.getMessage());
            return null;
        }
    }
}
