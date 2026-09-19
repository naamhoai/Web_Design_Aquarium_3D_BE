package com.aquarium.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantStockSummaryResponse {
    private UUID productVariantId;
    private Integer totalStock;
    private Integer totalReserved;
    private Integer totalAvailable;
    private Boolean inStock;
    private List<InventoryItemResponse> warehouseBreakdown;
}
