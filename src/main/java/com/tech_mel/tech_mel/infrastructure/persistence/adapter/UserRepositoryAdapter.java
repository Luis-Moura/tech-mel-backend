package com.tech_mel.tech_mel.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.output.UserRepositoryPort;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.UserEntity;
import com.tech_mel.tech_mel.infrastructure.persistence.mapper.UserMapper;
import com.tech_mel.tech_mel.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByVerificationToken(String token) {
        return userJpaRepository.findByVerificationToken(token)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAllWithAvailableHives(Pageable pageable) {
        return userJpaRepository.findByAvailableHivesGreaterThanAndRole(0, UserEntity.Role.COMMON, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userJpaRepository.findAll(pageable)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findByRole(User.Role role, Pageable pageable) {
        UserEntity.Role entityRole = mapRoleToEntity(role);
        return userJpaRepository.findByRole(entityRole, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findByIsActive(boolean isActive, Pageable pageable) {
        return userJpaRepository.findByIsActive(isActive, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String email, String name, Pageable pageable) {
        return userJpaRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(email, name, pageable)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(User.Role role) {
        UserEntity.Role entityRole = mapRoleToEntity(role);
        return userJpaRepository.findByRole(entityRole)
                .stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByIsPrimary(boolean isPrimary) {
        return userJpaRepository.findByIsPrimary(isPrimary)
                .stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByCreatedAtAfter(LocalDateTime date) {
        return userJpaRepository.findByCreatedAtAfter(date)
                .stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByLastLoginBefore(LocalDateTime date) {
        return userJpaRepository.findByLastLoginBefore(date)
                .stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByIsActiveAndRole(boolean isActive, User.Role role) {
        UserEntity.Role entityRole = mapRoleToEntity(role);
        return userJpaRepository.findByIsActiveAndRole(isActive, entityRole)
                .stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(User.Role role) {
        UserEntity.Role entityRole = mapRoleToEntity(role);
        return userJpaRepository.countByRole(entityRole);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByIsActive(boolean isActive) {
        return userJpaRepository.countByIsActive(isActive);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCreatedAtAfter(LocalDateTime date) {
        return userJpaRepository.countByCreatedAtAfter(date);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByLastLoginBefore(LocalDateTime date) {
        return userJpaRepository.countByLastLoginBefore(date);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return userJpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        userJpaRepository.deleteById(id);
    }

    // Método auxiliar para mapear Role do domínio para entidade
    private UserEntity.Role mapRoleToEntity(User.Role domainRole) {
        if (domainRole == null) {
            return null;
        }
        return switch (domainRole) {
            case COMMON -> UserEntity.Role.COMMON;
            case ADMIN -> UserEntity.Role.ADMIN;
            case TECHNICIAN -> UserEntity.Role.TECHNICIAN;
        };
    }
}