package com.tech_mel.tech_mel.infrastructure.persistence.mapper;

import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .name(entity.getName())
                .emailVerified(entity.isEmailVerified())
                .role(mapRoleToDomain(entity.getRole()))
                .enabled(entity.isEnabled())
                .locked(entity.isLocked())
                .verificationToken(entity.getVerificationToken())
                .tokenExpiry(entity.getTokenExpiry())
                .lastLogin(entity.getLastLogin())
                .authProvider(mapAuthProviderToDomain(entity.getAuthProvider()))
                .providerId(entity.getProviderId())
                .availableHives(entity.getAvailableHives())
                .isPrimary(entity.isPrimary())
                .isActive(entity.isActive())
                .requiresPasswordChange(entity.isRequiresPasswordChange())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        return UserEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .name(domain.getName())
                .emailVerified(domain.isEmailVerified())
                .role(mapRoleToEntity(domain.getRole()))
                .enabled(domain.isEnabled())
                .locked(domain.isLocked())
                .verificationToken(domain.getVerificationToken())
                .tokenExpiry(domain.getTokenExpiry())
                .lastLogin(domain.getLastLogin())
                .authProvider(mapAuthProviderToEntity(domain.getAuthProvider()))
                .providerId(domain.getProviderId())
                .availableHives(domain.getAvailableHives())
                .isPrimary(domain.isPrimary())
                .isActive(domain.isActive())
                .requiresPasswordChange(domain.isRequiresPasswordChange())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private User.Role mapRoleToDomain(UserEntity.Role entityRole) {
        if (entityRole == null) {
            return null;
        }
        return switch (entityRole) {
            case COMMON -> User.Role.COMMON;
            case ADMIN -> User.Role.ADMIN;
            case TECHNICIAN -> User.Role.TECHNICIAN;
        };
    }

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

    private User.AuthProvider mapAuthProviderToDomain(UserEntity.AuthProvider entityAuthProvider) {
        if (entityAuthProvider == null) {
            return null;
        }
        return switch (entityAuthProvider) {
            case LOCAL -> User.AuthProvider.LOCAL;
            case GOOGLE -> User.AuthProvider.GOOGLE;
        };
    }

    private UserEntity.AuthProvider mapAuthProviderToEntity(User.AuthProvider domainAuthProvider) {
        if (domainAuthProvider == null) {
            return null;
        }
        return switch (domainAuthProvider) {
            case LOCAL -> UserEntity.AuthProvider.LOCAL;
            case GOOGLE -> UserEntity.AuthProvider.GOOGLE;
        };
    }
}