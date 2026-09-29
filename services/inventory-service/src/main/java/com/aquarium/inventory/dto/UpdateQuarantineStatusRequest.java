package com.aquarium.inventory.dto;

import com.aquarium.inventory.entity.QuarantineStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuarantineStatusRequest {

    @NotNull(message = "status is required")
    private QuarantineStatus status;

    @Min(value = 0, message = "currentHealthyQuantity must be >= 0")
    @Max(value = 100000, message = "currentHealthyQuantity is too large")
    private Integer currentHealthyQuantity;

    @Size(max = 1000, message = "inspectionNotes is too long")
    private String inspectionNotes;
}
