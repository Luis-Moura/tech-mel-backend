package com.tech_mel.tech_mel.infrastructure.api.dto.response.users;

import java.time.LocalDateTime;
import java.util.UUID;
import com.tech_mel.tech_mel.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Dados do usuário")
public class UserResponse {
    @Schema(description = "ID único do usuário", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;
    
    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String name;
    
    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    private String email;
    
    @Schema(description = "Papel do usuário no sistema", example = "COMMON")
    private User.Role role;
    
    @Schema(description = "Se o email foi verificado", example = "true")
    private boolean emailVerified;
    
    @Schema(description = "Data de criação da conta", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Data do último login", example = "2024-01-02T15:30:00")
    private LocalDateTime lastLogin;
}
