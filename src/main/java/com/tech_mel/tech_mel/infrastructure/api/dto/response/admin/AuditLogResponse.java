package com.tech_mel.tech_mel.infrastructure.api.dto.response.admin;

import java.time.LocalDateTime;
import java.util.UUID;
import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private UUID id;
    
    private UUID userId;
    
    private String userName;
    
    private String userEmail;
    
    private AuditAction action;
    
    private EntityType entityType;
    
    private String entityId;
    
    private String details;
    
    private String ipAddress;
    
    private String userAgent;
    
    private LocalDateTime timestamp;
    
    private String oldValues;
    
    private String newValues;
    
    private boolean success;
    
    private String errorMessage;
}
