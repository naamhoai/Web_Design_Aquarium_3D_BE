package com.aquarium.aquarium3d.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "species_compatibilities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeciesCompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "species_a_id", nullable = false)
    private Integer speciesAId;

    @Column(name = "species_b_id", nullable = false)
    private Integer speciesBId;

    @Column(name = "is_compatible")
    @Builder.Default
    private Boolean isCompatible = true;

    @Column(name = "conflict_reason", columnDefinition = "TEXT")
    private String conflictReason;
}
