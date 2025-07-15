package com.tech_mel.tech_mel.infrastructure.api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Requisição para redefinir a senha do usuário.",
    example = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\", \"newPassword\": \"NovaSenhaForte123!\"}"
)
public record ResetPasswordRequest(
    @NotBlank
    @Schema(
        description = "Token recebido por e-mail para redefinição de senha.",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
    )
    String token,

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    @Schema(
        description = "Nova senha do usuário.",
        example = "NovaSenhaForte123!"
    )
    String newPassword
) {
}
