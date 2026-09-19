package com.aquarium.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mortality_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortalityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "death_count", nullable = false)
    private Integer deathCount;

    @Column(name = "cause_of_death")
    private String causeOfDeath;

    @Column(name = "temperature_recorded", precision = 4, scale = 1)
    private BigDecimal temperatureRecorded;

    @Column(name = "ph_recorded", precision = 3, scale = 1)
    private BigDecimal phRecorded;

    @Column(name = "logged_by")
    private UUID loggedBy;

    @CreationTimestamp
    @Column(name = "logged_at", updatable = false)
    private Instant loggedAt;
}
