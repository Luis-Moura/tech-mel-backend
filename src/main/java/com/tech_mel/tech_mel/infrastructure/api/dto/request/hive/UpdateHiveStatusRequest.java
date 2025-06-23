package com.tech_mel.tech_mel.infrastructure.api.dto.request.hive;

import com.tech_mel.tech_mel.domain.model.Hive;

public record UpdateHiveStatusRequest(
        Hive.HiveStatus hiveStatus
) {
}
