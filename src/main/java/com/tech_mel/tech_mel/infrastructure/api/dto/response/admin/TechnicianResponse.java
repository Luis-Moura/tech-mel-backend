package com.tech_mel.tech_mel.infrastructure.api.dto.response.admin;

import com.tech_mel.tech_mel.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianResponse {
    
    private UUID id;
    
    private String email;
    
    private String name;
    
    private User.Role role;
    
    private boolean isActive;
    
    private boolean isLocked;
    
    private boolean emailVerified;
    
    private boolean requiresPasswordChange;
    
    private int availableHives;
    
    private LocalDateTime lastLogin;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
