package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateHiveRequest(
        @NotBlank(message = "O nome da colmeia é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
        String name,

        @NotBlank(message = "A localização da colmeia é obrigatória.")
        @Size(min = 3, max = 255, message = "A localização deve ter entre 3 e 255 caracteres.")
        String location,

        UUID ownerId
) {
}
