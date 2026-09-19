package com.aquarium.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"warehouse_id", "product_variant_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Builder.Default
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Builder.Default
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Builder.Default
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public int getAvailableQuantity() {
        return Math.max(0, (stockQuantity != null ? stockQuantity : 0) - (reservedQuantity != null ? reservedQuantity : 0));
    }
}
