package com.tech_mel.tech_mel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Hive {
    private UUID id;

    private String name;

    private String location;

    private String apiKey;

    private HiveStatus hiveStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private User owner;

    public enum HiveStatus {
        INACTIVE, ACTIVE
    }
}
