package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para autenticação do usuário")
public record AuthRequest(
        @Schema(description = "Email do usuário", example = "usuario@exemplo.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "minhasenha123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String password
) {
}