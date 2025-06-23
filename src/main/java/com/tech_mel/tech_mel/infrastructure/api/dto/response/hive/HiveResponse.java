package com.tech_mel.tech_mel.infrastructure.api.dto.response.hive;

import java.util.UUID;
import com.tech_mel.tech_mel.domain.model.Hive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados da colmeia (visão completa para técnicos)")
public class HiveResponse {
    @Schema(description = "ID único da colmeia", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;
    
    @Schema(description = "Nome da colmeia", example = "Colmeia Principal")
    private String name;
    
    @Schema(description = "Localização da colmeia", example = "Apiário Norte - Setor A1")
    private String location;
    
    @Schema(description = "Chave de API da colmeia", example = "hive_key_abc123def456")
    private String apiKey;
    
    @Schema(description = "Status da colmeia", example = "ACTIVE")
    private Hive.HiveStatus hiveStatus;
    
    @Schema(description = "ID do proprietário da colmeia", example = "f47ac10b-58cc-4372-a567-0e02b2c3d478")
    private UUID ownerId;
}
