package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registro de novo usuário")
public record RegistrationRequest(
        @Schema(description = "Email do usuário", example = "usuario@exemplo.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "minhasenha123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
        String password,

        @Schema(description = "Nome completo do usuário", example = "João Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, message = "O nome deve ter pelo menos 3 caracteres")
        String name
) {
}