package com.aquarium.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInRequest {

    @NotNull(message = "warehouseId is required")
    private UUID warehouseId;

    @NotNull(message = "productVariantId is required")
    private UUID productVariantId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 100000, message = "quantity must be at most 100000")
    private Integer quantity;

    @Size(max = 500, message = "note is too long")
    private String note;
}
