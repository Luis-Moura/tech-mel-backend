package com.tech_mel.tech_mel.application.service;

import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.AuditLog;
import com.tech_mel.tech_mel.domain.model.EntityType;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AuditUseCase;
import com.tech_mel.tech_mel.domain.port.output.AuditRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService implements AuditUseCase {

    private final AuditRepositoryPort auditRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Async
    public void logAction(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            String entityId,
            String details
    )
    {
        CompletableFuture.completedFuture(logAction(userId, action, entityType, entityId, details, null, null));
    }

    @Override
    @Async
    public CompletableFuture<CompletableFuture<AuditLog>> logAction(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            String entityId,
            String details,
            String ipAddress,
            String userAgent) {
        return CompletableFuture.completedFuture(logAction(userId, action, entityType, entityId, details, null, null, ipAddress, userAgent));
    }

    @Override
    @Async
    public CompletableFuture<AuditLog> logAction(
            UUID userId,
            AuditAction action,
            EntityType entityType,
            String entityId,
            String details,
            String oldValues,
            String newValues,
            String ipAddress,
            String userAgent
    ) {
        try {
            User user = userRepositoryPort.findById(userId).orElse(null);

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userName(user != null ? user.getName() : "Unknown")
                    .userEmail(user != null ? user.getEmail() : "Unknown")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .timestamp(LocalDateTime.now())
                    .success(true)
                    .build();

            return CompletableFuture.completedFuture(auditRepositoryPort.save(auditLog));
        } catch (Exception e) {
            log.error("Erro ao salvar log de auditoria: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditRepositoryPort.findAll(pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByUser(UUID userId, Pageable pageable) {
        return auditRepositoryPort.findByUserId(userId, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByAction(AuditAction action, Pageable pageable) {
        return auditRepositoryPort.findByAction(action.name(), pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByEntityType(EntityType entityType, Pageable pageable) {
        return auditRepositoryPort.findByEntityType(entityType.name(), pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditRepositoryPort.findByTimestampBetween(startDate, endDate, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsWithFilters(UUID userId, AuditAction action, EntityType entityType,
                                                  LocalDateTime startDate, LocalDateTime endDate,
                                                  Boolean success, Pageable pageable) {
        return auditRepositoryPort.findByFilters(
                userId,
                action != null ? action.name() : null,
                entityType != null ? entityType.name() : null,
                startDate,
                endDate,
                success,
                pageable
        );
    }

    @Override
    public List<AuditLog> getRecentUserActivity(UUID userId, int limit) {
        return auditRepositoryPort.findRecentByUserId(userId, limit);
    }

    @Override
    public Map<String, Long> getAuditStatisticsByAction() {
        return java.util.Arrays.stream(AuditAction.values())
                .collect(Collectors.toMap(
                        AuditAction::name,
                        action -> auditRepositoryPort.countByAction(action.name())
                ));
    }

    @Override
    public Map<String, Long> getAuditStatisticsByEntityType() {
        return java.util.Arrays.stream(EntityType.values())
                .collect(Collectors.toMap(
                        EntityType::name,
                        entityType -> auditRepositoryPort.countByEntityType(entityType.name())
                ));
    }

    @Override
    public long getTotalAuditRecords() {
        return getAllAuditLogs(Pageable.unpaged()).getTotalElements();
    }

    @Override
    public long getFailedActionsCount() {
        return auditRepositoryPort.countBySuccess(false);
    }

    @Override
    public void cleanupOldAuditRecords(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        auditRepositoryPort.deleteOldRecords(cutoffDate);
        log.info("Limpeza de registros de auditoria concluída. Registros anteriores a {} foram removidos.", cutoffDate);
    }

    @Override
    public List<AuditLog> exportAuditLogs(LocalDateTime startDate, LocalDateTime endDate) {
        return getAuditLogsByDateRange(startDate, endDate, Pageable.unpaged()).getContent();
    }
}
