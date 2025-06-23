package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.infrastructure.security.oauth2.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "OAuth2", description = "Endpoints para autenticação OAuth2 com Google")
public class OAuth2Controller {

    private final RedisTemplate<String, Object> objectRedisTemplate;

    @GetMapping("/exchange-token")
    @Operation(
        summary = "Trocar state por tokens",
        description = "Troca o state temporário do OAuth2 pelos tokens de acesso e refresh após autenticação com Google"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tokens obtidos com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "State inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "O recurso solicitado não existe."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parâmetro state obrigatório",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Parâmetro state é obrigatório"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, String>> exchangeToken(
        @Parameter(description = "State temporário gerado durante o processo de autenticação OAuth2", required = true)
        @RequestParam String state
    ) {
        String redisKey = "oauth2:state:" + state;
        TokenPair tokenPair = (TokenPair) objectRedisTemplate.opsForValue().get(redisKey);

        if (tokenPair == null) {
            return ResponseEntity.notFound().build();
        }

        // Remover o token temporário após uso
        objectRedisTemplate.delete(redisKey);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", tokenPair.getAccessToken());
        response.put("refreshToken", tokenPair.getRefreshToken());

        return ResponseEntity.ok(response);
    }
}