package com.tech_mel.tech_mel.infrastructure.api.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de autenticação contendo tokens de acesso")
public class AuthResponse {
    @Schema(description = "Token de acesso JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "Token de refresh para renovação", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String refreshToken;
    
    @Schema(description = "Tipo do token", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "Tempo de expiração em segundos", example = "1800")
    private long expiresIn;
}