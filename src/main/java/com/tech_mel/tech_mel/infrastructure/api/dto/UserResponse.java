package com.tech_mel.tech_mel.infrastructure.api.dto;

import com.tech_mel.tech_mel.domain.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private User.Role role;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
