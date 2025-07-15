package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Requisição para atualizar a chave de API da colmeia.",
    example = "{\"apiKey\": \"f47ac10b-58cc-4372-a567-0e02b2c3d479\"}"
)
public record UpdateApiKeyRequest(
    @Schema(
        description = "Nova chave de API da colmeia.",
        example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
    )
    UUID apiKey
) {
}
