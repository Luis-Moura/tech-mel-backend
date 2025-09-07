package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.AuditLog;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLog toDomain(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return AuditLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .userEmail(entity.getUserEmail())
                .action(entity.getAction())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .timestamp(entity.getTimestamp())
                .oldValues(entity.getOldValues())
                .newValues(entity.getNewValues())
                .success(entity.isSuccess())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    public AuditLogEntity toEntity(AuditLog domain) {
        if (domain == null) {
            return null;
        }
        
        return AuditLogEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .userName(domain.getUserName())
                .userEmail(domain.getUserEmail())
                .action(domain.getAction())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .details(domain.getDetails())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .timestamp(domain.getTimestamp())
                .oldValues(domain.getOldValues())
                .newValues(domain.getNewValues())
                .success(domain.isSuccess())
                .errorMessage(domain.getErrorMessage())
                .build();
    }
}
