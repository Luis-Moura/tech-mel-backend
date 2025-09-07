package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.UserUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.users.ChangePasswordRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.users.UserResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints para gestão de dados do usuário")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserUseCase userUseCase;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("/me")
    @Operation(
        summary = "Obter dados do usuário atual",
        description = "Retorna os dados do usuário autenticado atualmente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dados do usuário retornados com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                        "name": "João Silva",
                        "email": "joao@example.com",
                        "role": "COMMON",
                        "emailVerified": true,
                        "createdAt": "2024-01-01T10:00:00",
                        "lastLogin": "2024-01-02T15:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de acesso inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token inválido ou expirado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Usuário não encontrado"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        // Usar o UUID como principal para busca direta
        UUID userId = authenticationUtil.getCurrentUserId();
        
        User currentUser = userUseCase.getCurrentUser(userId);

        UserResponse userResponse = UserResponse.builder()
                .id(currentUser.getId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .role(currentUser.getRole())
                .emailVerified(currentUser.isEmailVerified())
                .createdAt(currentUser.getCreatedAt())
                .lastLogin(currentUser.getLastLogin())
                .requiresPasswordChange(currentUser.isRequiresPasswordChange())
                .build();

        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/change-password")
    @Operation(
        summary = "Alterar senha do usuário",
        description = "Permite ao usuário alterar sua senha atual fornecendo a senha antiga e a nova senha"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Senha alterada com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "Senha alterada com sucesso!"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de acesso inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token inválido ou expirado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Usuário não encontrado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Erro de validação de senha",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Senha atual incorreta",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 409,
                            "error": "Conflict",
                            "message": "Senha atual incorreta"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Nova senha igual à atual",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 409,
                            "error": "Conflict",
                            "message": "A nova senha não pode ser igual à senha atual"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Usuário Google",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 409,
                            "error": "Conflict",
                            "message": "Usuários com login pelo Google não podem alterar a senha"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 400,
                        "error": "Validation Error",
                        "message": {
                            "newPassword": "A nova senha deve ter pelo menos 6 caracteres",
                            "oldPassword": "A senha atual é obrigatória"
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // Usar o email das claims do JWT
        String email = authenticationUtil.getCurrentUserEmail();

        userUseCase.changePassword(email, request.newPassword(), request.oldPassword());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
    }

    @PutMapping("/delete")
    @Operation(
        summary = "Excluir conta do usuário",
        description = "Realiza a exclusão lógica da conta do usuário (soft delete), desativando a conta e invalidando tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Conta excluída com sucesso"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de acesso inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token inválido ou expirado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Usuário não encontrado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Usuário já excluído",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 409,
                        "error": "Conflict",
                        "message": "Usuário já excluído"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Void> softDeleteUser() {
        // Usar o UUID como principal para busca direta
        UUID userId = authenticationUtil.getCurrentUserId();
        
        userUseCase.softDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}