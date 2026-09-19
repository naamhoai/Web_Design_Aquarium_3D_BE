package com.aquarium.inventory.dto;

import com.aquarium.inventory.entity.LivestockQuarantine;
import com.aquarium.inventory.entity.QuarantineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarantineBatchResponse {
    private UUID id;
    private UUID inventoryItemId;
    private String batchCode;
    private LocalDate arrivalDate;
    private LocalDate quarantineEndDate;
    private Integer initialQuantity;
    private Integer currentHealthyQuantity;
    private QuarantineStatus status;
    private String inspectionNotes;
    private Instant createdAt;

    public static QuarantineBatchResponse fromEntity(LivestockQuarantine q) {
        if (q == null) return null;
        return QuarantineBatchResponse.builder()
                .id(q.getId())
                .inventoryItemId(q.getInventoryItemId())
                .batchCode(q.getBatchCode())
                .arrivalDate(q.getArrivalDate())
                .quarantineEndDate(q.getQuarantineEndDate())
                .initialQuantity(q.getInitialQuantity())
                .currentHealthyQuantity(q.getCurrentHealthyQuantity())
                .status(q.getStatus())
                .inspectionNotes(q.getInspectionNotes())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
