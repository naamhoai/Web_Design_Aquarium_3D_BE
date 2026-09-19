package com.aquarium.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarantineBatchRequest {

    @NotNull(message = "inventoryItemId is required")
    private UUID inventoryItemId;

    @NotBlank(message = "batchCode is required")
    private String batchCode;

    @NotNull(message = "arrivalDate is required")
    private LocalDate arrivalDate;

    @NotNull(message = "quarantineEndDate is required")
    private LocalDate quarantineEndDate;

    @NotNull(message = "initialQuantity is required")
    @Min(value = 1, message = "initialQuantity must be at least 1")
    private Integer initialQuantity;

    private String inspectionNotes;
}
