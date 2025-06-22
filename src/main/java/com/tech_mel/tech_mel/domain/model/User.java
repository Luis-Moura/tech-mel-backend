package com.tech_mel.tech_mel.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class User {
    private UUID id;

    private String email;

    private String password;

    private String name;

    private boolean emailVerified;

    private Role role;

    private LocalDateTime lastLogin;

    private String verificationToken;

    private LocalDateTime tokenExpiry;

    private boolean locked;

    private boolean enabled;

    private AuthProvider authProvider;

    private String providerId;

    private int availableHives;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum AuthProvider {
        LOCAL,
        GOOGLE,
    }

    public enum Role {
        ADMIN,
        TECHNICIAN,
        COMMON
    }
}