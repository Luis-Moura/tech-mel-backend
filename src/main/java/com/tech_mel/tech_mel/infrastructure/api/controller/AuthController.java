package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.application.exception.UnauthorizedException;
import com.tech_mel.tech_mel.domain.model.RefreshToken;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.AuthUseCase;
import com.tech_mel.tech_mel.domain.port.input.RefreshTokenUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.auth.AuthRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.auth.ForgotPasswordRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.auth.RefreshTokenRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.auth.RegistrationRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.auth.ResendEmailVerificationRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.auth.ResetPasswordRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.auth.AuthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para autenticação e gestão de usuários")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostMapping("/login")
    @Operation(
        summary = "Autenticar usuário",
        description = "Realiza a autenticação do usuário com email e senha, retornando tokens de acesso e refresh"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login realizado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                        "tokenType": "Bearer",
                        "expiresIn": 1800
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciais inválidas ou email não verificado",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Credenciais inválidas",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Credenciais inválidas"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Email não verificado",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "E-mail não verificado. Por favor, verifique seu e-mail antes de fazer login."
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Conta bloqueada",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Conta bloqueada ou desativada. Entre em contato com o suporte."
                        }
                        """
                    )
                }
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
                        "error": "Not Found",\s
                        "message": "Credenciais inválidas"
                    }
                   \s"""
                )
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
                            "email": "O email é obrigatório",
                            "password": "A senha deve ter pelo menos 6 caracteres"
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        String accessToken = authUseCase.authenticateUser(request.email(), request.password());

        User user = authUseCase.findUserByEmail(request.email());
        RefreshToken refreshToken = refreshTokenUseCase.createRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
        summary = "Registrar novo usuário",
        description = "Registra um novo usuário no sistema e envia email de verificação"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuário registrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "Usuário registrado com sucesso. Verifique seu e-mail para ativar a conta."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email já cadastrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 409,
                        "error": "Conflict",
                        "message": "E-mail já cadastrado"
                    }
                    """
                )
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
                            "email": "Formato de email inválido",
                            "password": "A senha deve ter pelo menos 6 caracteres",
                            "name": "O nome deve ter pelo menos 3 caracteres"
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        authUseCase.registerUser(request.email(), request.password(), request.name());
        return ResponseEntity.ok().body(Map.of("message", "Usuário registrado com sucesso. Verifique seu e-mail para ativar a conta."));
    }

    @PostMapping("/resend-verification-email")
    @Operation(
        summary = "Reenviar email de verificação",
        description = "Reenvia o email de verificação para usuários não verificados"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email de verificação reenviado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "E-mail de verificação reenviado com sucesso."
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
                        "message": "Usuário não encontrado com o email: user@example.com"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email já verificado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 409,
                        "error": "Conflict",
                        "message": "E-mail já verificado"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestParam ResendEmailVerificationRequest request) {
        authUseCase.resendVerificationEmail(request.email());
        return ResponseEntity.ok().body(Map.of("message", "E-mail de verificação reenviado com sucesso."));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Renovar token de acesso",
        description = "Renova o token de acesso usando um refresh token válido"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token renovado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                        "tokenType": "Bearer",
                        "expiresIn": 1800
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Refresh token inválido ou expirado"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = authUseCase.refreshToken(request.refreshToken());

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.refreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    @Operation(
        summary = "Verificar email do usuário",
        description = "Verifica o email do usuário usando o token de verificação enviado por email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email verificado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "E-mail verificado com sucesso. Agora você pode fazer login."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de verificação inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token de verificação inválido ou expirado"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> verifyEmail(
        @Parameter(description = "Token de verificação recebido por email", required = true)
        @RequestParam String token
    ) {
        authUseCase.verifyEmail(token);

        return ResponseEntity.ok().body(Map.of("message", "E-mail verificado com sucesso. Agora você pode fazer login."));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Realizar logout",
        description = "Realiza o logout do usuário invalidando o token de acesso e todos os refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout realizado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "Logout realizado com sucesso"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token inválido ou malformado",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Cabeçalho inválido",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Cabeçalho de autorização inválido"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Token inválido",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Token inválido ou expirado"
                        }
                        """
                    )
                }
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
    public ResponseEntity<?> logout(
        @Parameter(description = "Token de acesso no formato 'Bearer {token}'", required = true)
        @RequestHeader("Authorization") String authHeader
    ) {
        // Valida o formato do cabeçalho de autorização
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Cabeçalho de autorização inválido");
        }

        String token = authHeader.substring(7);
        authUseCase.logout(token);

        return ResponseEntity.ok().body(Map.of("message", "Logout realizado com sucesso"));
    }

    @GetMapping("/forgot-password/request")
    @Operation(
        summary = "Solicitar redefinição de senha",
        description = "Solicita a redefinição de senha enviando um email com token de redefinição"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email de redefinição enviado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "E-mail de redefinição de senha enviado com sucesso."
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
                        "message": "Usuário não encontrado com o email: user@example.com"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Usuário não tem permissão para redefinir senha",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Usuário não cadastrado com e-mail e senha"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authUseCase.requestPasswordReset(request.email());

        return ResponseEntity.ok().body(Map.of("message", "E-mail de redefinição de senha enviado com sucesso."));
    }

    @PostMapping("/forgot-password/reset")
    @Operation(
        summary = "Redefinir senha",
        description = "Redefine a senha do usuário usando o token de redefinição recebido por email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Senha redefinida com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "message": "Senha redefinida com sucesso."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de redefinição inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Token inválido",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Token de redefinição de senha inválido ou expirado"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Conta Google",
                        value = """
                        {
                            "timestamp": "2024-01-01T10:00:00",
                            "status": 401,
                            "error": "Unauthorized",
                            "message": "Usuário não cadastrado com e-mail e senha"
                        }
                        """
                    )
                }
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
                            "token": "Token é obrigatório",
                            "newPassword": "A nova senha deve ter pelo menos 6 caracteres"
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(UUID.fromString(request.token()), request.newPassword());

        return ResponseEntity.ok().body(Map.of("message", "Senha redefinida com sucesso."));
    }
}