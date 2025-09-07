package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.AuditLog;
import com.tech_mel.tech_mel.domain.model.EntityType;
import com.tech_mel.tech_mel.domain.port.output.AuditRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.AuditLogEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.AuditLogMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditRepositoryAdapter implements AuditRepositoryPort {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = auditLogMapper.toEntity(auditLog);
        AuditLogEntity savedEntity = auditLogJpaRepository.save(entity);
        return auditLogMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findAll(pageable);
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByUserId(UUID userId, Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findByUserId(userId, pageable);
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByAction(String action, Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findByAction(AuditAction.valueOf(action), pageable);
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntityType(String entityType, Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findByEntityType(EntityType.valueOf(entityType), pageable);
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntityId(String entityId, Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findByEntityId(entityId, pageable);
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findByTimestampBetween(startTime, endTime, pageable);
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> findByFilters(UUID userId, String action, String entityType,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       Boolean success, Pageable pageable) {
        Page<AuditLogEntity> entityPage = auditLogJpaRepository.findByFilters(
            userId, action, entityType, startTime, endTime, success, pageable
        );
        return entityPage.map(auditLogMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findRecentByUserId(UUID userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<AuditLogEntity> entities = auditLogJpaRepository.findRecentByUserId(userId, pageable);
        return entities.stream()
                .map(auditLogMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAction(String action) {
        return auditLogJpaRepository.countByAction(AuditAction.valueOf(action));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByEntityType(String entityType) {
        return auditLogJpaRepository.countByEntityType(EntityType.valueOf(entityType));
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySuccess(boolean success) {
        return auditLogJpaRepository.countBySuccess(success);
    }

    @Override
    @Transactional
    public void deleteOldRecords(LocalDateTime cutoffDate) {
        auditLogJpaRepository.deleteByTimestampBefore(cutoffDate);
    }
}
