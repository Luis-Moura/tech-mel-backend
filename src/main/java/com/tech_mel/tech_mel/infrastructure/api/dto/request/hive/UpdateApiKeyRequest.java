package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateApiKeyRequest(
        UUID apiKey
) {
}
