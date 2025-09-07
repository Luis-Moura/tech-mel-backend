package com.tech_mel.tech_mel.domain.port.output;

import com.tech_mel.tech_mel.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditRepositoryPort {
    
    AuditLog save(AuditLog auditLog);
    
    Page<AuditLog> findAll(Pageable pageable);
    
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
    
    Page<AuditLog> findByEntityId(String entityId, Pageable pageable);
    
    Page<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    Page<AuditLog> findByFilters(
            UUID userId,
            String action,
            String entityType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean success, Pageable pageable);
    
    List<AuditLog> findRecentByUserId(UUID userId, int limit);
    
    long countByAction(String action);
    
    long countByEntityType(String entityType);
    
    long countBySuccess(boolean success);
    
    void deleteOldRecords(LocalDateTime cutoffDate);
}
