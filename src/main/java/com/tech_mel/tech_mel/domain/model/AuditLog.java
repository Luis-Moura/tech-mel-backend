package com.tech_mel.tech_mel.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {
    
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
