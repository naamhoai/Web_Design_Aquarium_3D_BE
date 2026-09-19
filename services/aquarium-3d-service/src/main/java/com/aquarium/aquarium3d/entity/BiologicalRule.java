package com.aquarium.aquarium3d.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "biological_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiologicalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "species_name", nullable = false, unique = true, length = 150)
    private String speciesName;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "min_tank_liters", nullable = false, precision = 6, scale = 1)
    private BigDecimal minTankLiters;

    @Column(name = "ph_min", nullable = false, precision = 3, scale = 1)
    private BigDecimal phMin;

    @Column(name = "ph_max", nullable = false, precision = 3, scale = 1)
    private BigDecimal phMax;

    @Column(name = "temp_min", nullable = false, precision = 4, scale = 1)
    private BigDecimal tempMin;

    @Column(name = "temp_max", nullable = false, precision = 4, scale = 1)
    private BigDecimal tempMax;

    @Column(name = "bioload_factor", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal bioloadFactor = new BigDecimal("1.00");

    @Column(name = "aggressiveness_level")
    @Builder.Default
    private Integer aggressivenessLevel = 1; // 1 (peaceful) to 5 (predator)

    @Column(name = "school_min_quantity")
    @Builder.Default
    private Integer schoolMinQuantity = 1;

    @Column(name = "swimming_layer", length = 50)
    @Builder.Default
    private String swimmingLayer = "MID"; // TOP, MID, BOTTOM

    @Column(name = "caution_notes", columnDefinition = "TEXT")
    private String cautionNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
