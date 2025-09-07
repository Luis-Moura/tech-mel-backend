package com.tech_mel.tech_mel.domain.port.input;

import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.AuditLog;
import com.tech_mel.tech_mel.domain.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AuditUseCase {

    // Operações básicas de auditoria
    void logAction(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            String entityId,
            String details
    );

    CompletableFuture<CompletableFuture<AuditLog>> logAction(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            String entityId,
            String details,
            String ipAddress,
            String userAgent
    );

    CompletableFuture<AuditLog> logAction(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            String entityId,
            String details,
            String oldValues,
            String newValues,
            String ipAddress,
            String userAgent
    );

    // Consultas de auditoria
    Page<AuditLog> getAllAuditLogs(Pageable pageable);

    Page<AuditLog> getAuditLogsByUser(UUID userId, Pageable pageable);

    Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable);

    Page<AuditLog> getAuditLogsByEntityType(EntityType entityType, Pageable pageable);

    Page<AuditLog> getAuditLogsByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    Page<AuditLog> getAuditLogsWithFilters(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Boolean success, Pageable pageable);

    List<AuditLog> getRecentUserActivity(UUID userId, int limit);

    // Estatísticas de auditoria
    Map<String, Long> getAuditStatisticsByAction();

    Map<String, Long> getAuditStatisticsByEntityType();

    long getTotalAuditRecords();

    long getFailedActionsCount();

    // Manutenção
    void cleanupOldAuditRecords(int retentionDays);

    // Exportação
    List<AuditLog> exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate);
}
