package com.tech_mel.tech_mel.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Entity
@Table(name = "threshold")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThresholdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "temperature_min", nullable = false)
    private Double temperatureMin;

    @Column(name = "temperature_max", nullable = false)
    private Double temperatureMax;

    @Column(name = "humidity_min", nullable = false)
    private Double humidityMin;

    @Column(name = "humidity_max", nullable = false)
    private Double humidityMax;

    @Column(name = "co2_min", nullable = false)
    private Double co2Min;

    @Column(name = "co2_max", nullable = false)
    private Double co2Max;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HiveEntity hive;
}

