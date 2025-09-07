package com.tech_mel.tech_mel.infrastructure.api.dto.request.admin;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.EntityType;
import lombok.Data;

@Data
public class AuditLogFilterRequest {
    
    private UUID userId;
    
    private String userEmail;
    
    private AuditAction action;
    
    private EntityType entityType;
    
    private String entityId;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    
    private Boolean success;
    
    private String ipAddress;
    
    private String searchTerm; // Busca geral em detalhes, userName, userEmail
}
