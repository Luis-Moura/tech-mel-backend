package com.tech_mel.tech_mel.infrastructure.api.dto.response.hive;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados de usuários que possuem colmeias disponíveis para criação")
public class UsersWithAvailableHivesResponse {
    @Schema(description = "ID único do usuário", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;

    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String name;

    @Schema(description = "Email do usuário", example = "joao@exemplo.com")
    private String email;

    @Schema(description = "Número de colmeias disponíveis para criação", example = "5")
    private int availableHives;
}
