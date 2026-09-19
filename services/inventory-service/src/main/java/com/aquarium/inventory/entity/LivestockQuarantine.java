package com.aquarium.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "livestock_quarantines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivestockQuarantine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "quarantine_end_date", nullable = false)
    private LocalDate quarantineEndDate;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "current_healthy_quantity", nullable = false)
    private Integer currentHealthyQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "quarantine_status_enum")
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private QuarantineStatus status = QuarantineStatus.IN_QUARANTINE;

    @Column(name = "inspection_notes", columnDefinition = "TEXT")
    private String inspectionNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
