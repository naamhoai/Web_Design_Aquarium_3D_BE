package com.aquarium.inventory.dto;

import com.aquarium.inventory.entity.InventoryItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {
    private UUID id;
    private UUID warehouseId;
    private UUID productVariantId;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private Instant updatedAt;

    public static InventoryItemResponse fromEntity(InventoryItem item) {
        if (item == null) return null;
        int available = item.getAvailableQuantity();
        return InventoryItemResponse.builder()
                .id(item.getId())
                .warehouseId(item.getWarehouseId())
                .productVariantId(item.getProductVariantId())
                .stockQuantity(item.getStockQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableQuantity(available)
                .lowStockThreshold(item.getLowStockThreshold())
                .isLowStock(available <= (item.getLowStockThreshold() != null ? item.getLowStockThreshold() : 0))
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
