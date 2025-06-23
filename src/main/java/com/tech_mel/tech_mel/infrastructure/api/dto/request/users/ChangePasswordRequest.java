package com.tech_mel.tech_mel.infrastructure.api.dto.request.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para alteração de senha")
public record ChangePasswordRequest(
        @Schema(description = "Senha atual do usuário", example = "senhaatual123")
        @NotBlank
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String oldPassword,

        @Schema(description = "Nova senha do usuário", example = "novasenha456")
        @NotBlank
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String newPassword
) {
}
