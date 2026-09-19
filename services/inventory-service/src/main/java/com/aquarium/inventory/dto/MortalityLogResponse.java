package com.aquarium.inventory.dto;

import com.aquarium.inventory.entity.MortalityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MortalityLogResponse {
    private Long id;
    private UUID inventoryItemId;
    private Integer deathCount;
    private String causeOfDeath;
    private BigDecimal temperatureRecorded;
    private BigDecimal phRecorded;
    private UUID loggedBy;
    private Instant loggedAt;
    private Integer remainingStockQuantity;

    public static MortalityLogResponse fromEntity(MortalityLog log, Integer remainingStock) {
        if (log == null) return null;
        return MortalityLogResponse.builder()
                .id(log.getId())
                .inventoryItemId(log.getInventoryItemId())
                .deathCount(log.getDeathCount())
                .causeOfDeath(log.getCauseOfDeath())
                .temperatureRecorded(log.getTemperatureRecorded())
                .phRecorded(log.getPhRecorded())
                .loggedBy(log.getLoggedBy())
                .loggedAt(log.getLoggedAt())
                .remainingStockQuantity(remainingStock)
                .build();
    }
}
