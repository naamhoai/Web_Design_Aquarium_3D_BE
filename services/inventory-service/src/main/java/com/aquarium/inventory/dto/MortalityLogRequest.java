package com.aquarium.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortalityLogRequest {

    @NotNull(message = "inventoryItemId is required")
    private UUID inventoryItemId;

    @NotNull(message = "deathCount is required")
    @Min(value = 1, message = "deathCount must be at least 1")
    private Integer deathCount;

    private String causeOfDeath;
    private BigDecimal temperatureRecorded;
    private BigDecimal phRecorded;
    private UUID loggedBy;
}
