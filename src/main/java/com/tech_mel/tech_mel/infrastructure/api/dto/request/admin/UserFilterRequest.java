package com.tech_mel.tech_mel.infrastructure.api.dto.request.admin;

import com.tech_mel.tech_mel.domain.model.User;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class UserFilterRequest {
    
    private String searchTerm; // Busca por email ou nome
    
    private User.Role role;
    
    private Boolean isActive;
    
    private Boolean isLocked;
    
    private Boolean emailVerified;
    
    private User.AuthProvider authProvider;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAfter;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdBefore;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastLoginAfter;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastLoginBefore;
    
    private Integer minAvailableHives;
    
    private Integer maxAvailableHives;
}
