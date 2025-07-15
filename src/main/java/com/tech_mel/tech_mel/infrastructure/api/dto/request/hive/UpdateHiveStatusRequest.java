package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import com.tech_mel.tech_mel.domain.model.Hive;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Requisição para atualizar o status da colmeia.",
    example = "{\"hiveStatus\": \"ACTIVE\"}"
)
public record UpdateHiveStatusRequest(
    @Schema(
        description = "Novo status da colmeia.",
        example = "ACTIVE"
    )
    Hive.HiveStatus hiveStatus
) {
}
