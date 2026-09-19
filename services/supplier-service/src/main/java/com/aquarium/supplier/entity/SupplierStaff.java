package com.aquarium.supplier.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supplier_staff")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "permissions", columnDefinition = "JSONB")
    @Builder.Default
    private String permissions = "[\"INVENTORY_READ\", \"ORDER_READ\"]";

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
