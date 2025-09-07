package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminUseCase {
    
    // Gestão de Técnicos
    User createTechnician(String email, String name, String password);
    
    User updateTechnician(UUID technicianId, String name, String email, Integer availableHives);
    
    void deleteTechnician(UUID technicianId);
    
    List<User> getAllTechnicians();
    
    // Gestão de Administradores Secundários
    User createSecondaryAdmin(String email, String name, String password);
    
    List<User> getAllAdmins();
    
    // Gestão Geral de Usuários
    Page<User> getAllUsers(Pageable pageable);
    
    Page<User> getUsersByRole(User.Role role, Pageable pageable);
    
    Page<User> getUsersByStatus(boolean isActive, Pageable pageable);
    
    Page<User> searchUsers(String searchTerm, Pageable pageable);
    
    void activateUser(UUID userId);
    
    void deactivateUser(UUID userId);
    
    void resetUserPassword(UUID userId);
    
    void forcePasswordChange(UUID userId);
    
    void unlockUser(UUID userId);
    
    void lockUser(UUID userId);
    
    // Estatísticas do Sistema
    Map<String, Object> getSystemStatistics();
    
    Map<String, Long> getUserStatisticsByRole();
    
    Map<String, Long> getActiveUsersStatistics();
    
    List<User> getRecentlyRegisteredUsers(int limit);
    
    List<User> getInactiveUsers(int daysInactive);
    
    // Validações de Permissão
    boolean isPrimaryAdmin(UUID userId);
    
    boolean canManageAdmins(UUID currentUserId);
    
    boolean canManageUser(UUID currentUserId, UUID targetUserId);
}
