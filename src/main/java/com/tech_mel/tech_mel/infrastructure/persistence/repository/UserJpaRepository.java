package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.UserEntity;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByVerificationToken(String token);

    Page<UserEntity> findByAvailableHivesGreaterThanAndRole(int availableHives, UserEntity.Role role, Pageable pageable);
    
    // Novos métodos para administração
    Page<UserEntity> findByRole(UserEntity.Role role, Pageable pageable);
    
    Page<UserEntity> findByIsActive(boolean isActive, Pageable pageable);
    
    Page<UserEntity> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name, Pageable pageable);
    
    List<UserEntity> findByRole(UserEntity.Role role);
    
    List<UserEntity> findByIsPrimary(boolean isPrimary);
    
    List<UserEntity> findByCreatedAtAfter(LocalDateTime date);
    
    List<UserEntity> findByLastLoginBefore(LocalDateTime date);
    
    List<UserEntity> findByIsActiveAndRole(boolean isActive, UserEntity.Role role);
    
    long countByRole(UserEntity.Role role);
    
    long countByIsActive(boolean isActive);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    long countByLastLoginBefore(LocalDateTime date);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndIdNot(String email, UUID id);
    
    @Query("SELECT u FROM UserEntity u WHERE u.lastLogin < :cutoffDate OR u.lastLogin IS NULL")
    List<UserEntity> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt >= :startDate ORDER BY u.createdAt DESC")
    List<UserEntity> findRecentlyRegistered(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}