package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.domain.model.AuditAction;
import com.tech_mel.tech_mel.domain.model.EntityType;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLogEntity> findByAction(AuditAction action, Pageable pageable);

    Page<AuditLogEntity> findByEntityType(EntityType entityType, Pageable pageable);

    Page<AuditLogEntity> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR a.timestamp <= :endTime) AND " +
           "(:success IS NULL OR a.success = :success)")
    Page<AuditLogEntity> findByFilters(@Param("userId") UUID userId,
                                      @Param("action") String action,
                                      @Param("entityType") String entityType,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("success") Boolean success,
                                      Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    List<AuditLogEntity> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByAction(AuditAction action);

    long countByEntityType(EntityType entityType);

    long countBySuccess(boolean success);

    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.timestamp < :cutoffDate")
    void deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
