package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação de nova colmeia")
public record CreateHiveRequest(
        @Schema(description = "Nome da colmeia", example = "Colmeia Principal")
        @NotBlank(message = "O nome da colmeia é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
        String name,

        @Schema(description = "Localização da colmeia", example = "Apiário Norte - Setor A1")
        @NotBlank(message = "A localização da colmeia é obrigatória.")
        @Size(min = 3, max = 255, message = "A localização deve ter entre 3 e 255 caracteres.")
        String location,

        @Schema(description = "ID do usuário proprietário da colmeia", example = "f47ac10b-58cc-4372-a567-0e02b2c3d478")
        UUID ownerId
) {
}
