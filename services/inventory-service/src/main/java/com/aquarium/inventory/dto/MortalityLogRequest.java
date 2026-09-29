package com.aquarium.inventory.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Max(value = 100000, message = "deathCount is too large")
    private Integer deathCount;

    @Size(max = 255, message = "causeOfDeath is too long")
    private String causeOfDeath;

    @DecimalMin(value = "-5.0", message = "temperatureRecorded is out of range")
    @DecimalMax(value = "50.0", message = "temperatureRecorded is out of range")
    private BigDecimal temperatureRecorded;

    @DecimalMin(value = "0.0", message = "phRecorded must be between 0 and 14")
    @DecimalMax(value = "14.0", message = "phRecorded must be between 0 and 14")
    private BigDecimal phRecorded;
}
