package com.tech_mel.tech_mel.infrastructure.api.dto.response.hive;

import com.tech_mel.tech_mel.domain.model.Hive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HiveResponse {
    private UUID id;
    private String name;
    private String location;
    private String apiKey;
    private Hive.HiveStatus hiveStatus;
    private UUID ownerId;
}
