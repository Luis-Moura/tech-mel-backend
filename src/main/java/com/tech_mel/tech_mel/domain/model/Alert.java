package com.tech_mel.tech_mel.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Alert {
    private UUID id;
    private LocalDateTime timestamp;
    private AlertType type; // TEMPERATURE, HUMIDITY, CO2
    private AlertSeverity severity; // LOW, MEDIUM, HIGH
    private Double value; // valor que gerou o alerta
    private AlertStatus status; // NEW, VIEWED, RESOLVED
    private Hive hive;

    public enum AlertType {
        TEMPERATURE,
        HUMIDITY,
        CO2
    }

    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum AlertStatus {
        NEW,
        VIEWED,
        RESOLVED
    }
}

