package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AdminUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.admin.CreateAdminRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.admin.CreateTechnicianRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.admin.UpdateTechnicianRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.admin.UserFilterRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.admin.SystemStatisticsResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.admin.TechnicianResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.admin.UserListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Operações administrativas do sistema")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUseCase adminUseCase;

    // ========== GESTÃO DE TÉCNICOS ==========

    @PostMapping("/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar novo técnico", description = "Cria um novo técnico no sistema")
    public ResponseEntity<TechnicianResponse> createTechnician(@Valid @RequestBody CreateTechnicianRequest request) {
        log.info("Criando novo técnico: {}", request.getEmail());
        
        User technician = adminUseCase.createTechnician(
            request.getEmail(),
            request.getName(),
            request.getPassword()
        );
        
        TechnicianResponse response = mapToTechnicianResponse(technician);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/technicians/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar técnico", description = "Atualiza as informações de um técnico")
    public ResponseEntity<TechnicianResponse> updateTechnician(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTechnicianRequest request
    ) {
        
        log.info("Atualizando técnico: {}", id);
        
        User technician = adminUseCase.updateTechnician(
            id,
            request.getName(),
            request.getEmail(),
            request.getAvailableHives()
        );
        
        TechnicianResponse response = mapToTechnicianResponse(technician);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/technicians/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deletar técnico", description = "Remove um técnico do sistema")
    public ResponseEntity<Void> deleteTechnician(@PathVariable UUID id) {
        log.info("Deletando técnico: {}", id);
        adminUseCase.deleteTechnician(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar técnicos", description = "Retorna todos os técnicos do sistema")
    public ResponseEntity<List<TechnicianResponse>> getAllTechnicians() {
        log.info("Buscando todos os técnicos");
        
        List<User> technicians = adminUseCase.getAllTechnicians();
        List<TechnicianResponse> response = technicians.stream()
                .map(this::mapToTechnicianResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    // ========== GESTÃO DE ADMINISTRADORES SECUNDÁRIOS ==========

    @PostMapping("/secondary-admins")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar administrador secundário", description = "Cria um novo administrador secundário (apenas admin primário)")
    public ResponseEntity<UserListResponse> createSecondaryAdmin(@Valid @RequestBody CreateAdminRequest request) {
        log.info("Criando administrador secundário: {}", request.getEmail());
        
        User admin = adminUseCase.createSecondaryAdmin(
            request.getEmail(),
            request.getName(),
            request.getPassword()
        );
        
        UserListResponse response = mapToUserListResponse(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar administradores", description = "Retorna todos os administradores do sistema")
    public ResponseEntity<List<UserListResponse>> getAllAdmins() {
        log.info("Buscando todos os administradores");
        
        List<User> admins = adminUseCase.getAllAdmins();
        List<UserListResponse> response = admins.stream()
                .map(this::mapToUserListResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    // ========== GESTÃO GERAL DE USUÁRIOS ==========

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários", description = "Lista todos os usuários com paginação e filtros")
    public ResponseEntity<Page<UserListResponse>> getAllUsers(
            @Parameter(description = "Página (inicia em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "desc") String sortDirection,
            UserFilterRequest filters) {
        
        log.info("Buscando usuários - página: {}, tamanho: {}", page, size);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<User> users;
        if (filters.getSearchTerm() != null && !filters.getSearchTerm().trim().isEmpty()) {
            users = adminUseCase.searchUsers(filters.getSearchTerm(), pageable);
        } else if (filters.getRole() != null) {
            users = adminUseCase.getUsersByRole(filters.getRole(), pageable);
        } else if (filters.getIsActive() != null) {
            users = adminUseCase.getUsersByStatus(filters.getIsActive(), pageable);
        } else {
            users = adminUseCase.getAllUsers(pageable);
        }
        
        Page<UserListResponse> response = users.map(this::mapToUserListResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativar usuário", description = "Ativa um usuário inativo")
    public ResponseEntity<Void> activateUser(@PathVariable UUID id) {
        log.info("Ativando usuário: {}", id);
        adminUseCase.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário ativo")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        log.info("Desativando usuário: {}", id);
        adminUseCase.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resetar senha", description = "Reseta a senha de um usuário")
    public ResponseEntity<Void> resetUserPassword(@PathVariable UUID id) {
        log.info("Resetando senha do usuário: {}", id);
        
        // Validação adicional será feita no AdminService
        adminUseCase.resetUserPassword(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/force-password-change")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Forçar mudança de senha", description = "Força um usuário a alterar sua senha no próximo login")
    public ResponseEntity<Void> forcePasswordChange(@PathVariable UUID id) {
        log.info("Forçando mudança de senha para usuário: {}", id);
        adminUseCase.forcePasswordChange(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desbloquear usuário", description = "Desbloqueia um usuário bloqueado")
    public ResponseEntity<Void> unlockUser(@PathVariable UUID id) {
        log.info("Desbloqueando usuário: {}", id);
        adminUseCase.unlockUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bloquear usuário", description = "Bloqueia um usuário")
    public ResponseEntity<Void> lockUser(@PathVariable UUID id) {
        log.info("Bloqueando usuário: {}", id);
        adminUseCase.lockUser(id);
        return ResponseEntity.ok().build();
    }

    // ========== ESTATÍSTICAS DO SISTEMA ==========

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estatísticas do sistema", description = "Retorna estatísticas gerais do sistema")
    public ResponseEntity<SystemStatisticsResponse> getSystemStatistics() {
        log.info("Buscando estatísticas do sistema");
        
        Map<String, Object> stats = adminUseCase.getSystemStatistics();
        Map<String, Long> usersByRole = adminUseCase.getUserStatisticsByRole();
        Map<String, Long> activeStats = adminUseCase.getActiveUsersStatistics();
        
        SystemStatisticsResponse response = SystemStatisticsResponse.builder()
                .totalUsers((Long) stats.get("totalUsers"))
                .activeUsers((Long) stats.get("activeUsers"))
                .inactiveUsers((Long) stats.get("inactiveUsers"))
                .adminUsers((Long) stats.get("admins"))
                .technicianUsers((Long) stats.get("technicians"))
                .commonUsers((Long) stats.get("commonUsers"))
                .newUsersLastMonth((Long) stats.get("newUsersLastMonth"))
                .usersByRole(usersByRole)
                .usersByStatus(activeStats)
                .generatedAt(LocalDateTime.now())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics/recent-users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Usuários recentes", description = "Retorna usuários registrados recentemente")
    public ResponseEntity<List<UserListResponse>> getRecentlyRegisteredUsers(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Buscando usuários recentes - limite: {}", limit);
        
        List<User> recentUsers = adminUseCase.getRecentlyRegisteredUsers(limit);
        List<UserListResponse> response = recentUsers.stream()
                .map(this::mapToUserListResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics/inactive-users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Usuários inativos", description = "Retorna usuários que não fazem login há X dias")
    public ResponseEntity<List<UserListResponse>> getInactiveUsers(
            @RequestParam(defaultValue = "30") int daysInactive) {
        
        log.info("Buscando usuários inativos - dias: {}", daysInactive);
        
        List<User> inactiveUsers = adminUseCase.getInactiveUsers(daysInactive);
        List<UserListResponse> response = inactiveUsers.stream()
                .map(this::mapToUserListResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private TechnicianResponse mapToTechnicianResponse(User user) {
        return TechnicianResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .isActive(user.isActive())
                .isLocked(user.isLocked())
                .emailVerified(user.isEmailVerified())
                .requiresPasswordChange(user.isRequiresPasswordChange())
                .availableHives(user.getAvailableHives())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserListResponse mapToUserListResponse(User user) {
        return UserListResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .isActive(user.isActive())
                .isLocked(user.isLocked())
                .emailVerified(user.isEmailVerified())
                .isPrimary(user.isPrimary())
                .requiresPasswordChange(user.isRequiresPasswordChange())
                .authProvider(user.getAuthProvider())
                .availableHives(user.getAvailableHives())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
