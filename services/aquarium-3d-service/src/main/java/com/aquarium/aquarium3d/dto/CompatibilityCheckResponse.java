package com.aquarium.aquarium3d.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityCheckResponse {
    private boolean isCompatible;
    private Double totalBioLoad;
    private Double maxBioLoadCapacity;
    private boolean isBioLoadSafe;
    private List<String> warnings;
    private String recommendedPhRange;
    private String recommendedTempRange;
}
