package com.tech_mel.tech_mel.infrastructure.persistence.repository;

import com.tech_mel.tech_mel.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    
    long countByRole(UserEntity.Role role);
    
    long countByIsActive(boolean isActive);
    
    long countByCreatedAtAfter(LocalDateTime date);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndIdNot(String email, UUID id);
}