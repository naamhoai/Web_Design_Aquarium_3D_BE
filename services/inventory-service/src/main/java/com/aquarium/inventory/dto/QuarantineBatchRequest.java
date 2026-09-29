package com.aquarium.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,100}$", message = "batchCode must be 3-100 letters, digits, - or _")
    private String batchCode;

    @NotNull(message = "arrivalDate is required")
    private LocalDate arrivalDate;

    @NotNull(message = "quarantineEndDate is required")
    private LocalDate quarantineEndDate;

    @NotNull(message = "initialQuantity is required")
    @Min(value = 1, message = "initialQuantity must be at least 1")
    @Max(value = 100000, message = "initialQuantity is too large")
    private Integer initialQuantity;

    @Size(max = 1000, message = "inspectionNotes is too long")
    private String inspectionNotes;
}
