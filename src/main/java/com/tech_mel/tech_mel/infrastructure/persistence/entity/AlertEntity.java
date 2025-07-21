package com.tech_mel.tech_mel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HiveEntity hive;

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

