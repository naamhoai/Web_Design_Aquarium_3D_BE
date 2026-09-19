package com.aquarium.inventory.dto;

import com.aquarium.inventory.entity.QuarantineStatus;
import jakarta.validation.constraints.NotNull;
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

    private Integer currentHealthyQuantity;
    private String inspectionNotes;
}
