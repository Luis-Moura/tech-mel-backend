package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import java.util.UUID;

public record UpdateApiKeyRequest(
        UUID apiKey
) {
}
