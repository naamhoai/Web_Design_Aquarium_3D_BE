package com.aquarium.aquarium3d.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityCheckResponse {
    /** Đặt tên field không có tiền tố "is" + @JsonProperty để JSON luôn là "isCompatible" (trước đây bị đổi thành "compatible"). */
    @JsonProperty("isCompatible")
    private boolean compatible;
    private Double totalBioLoad;
    private Double maxBioLoadCapacity;
    @JsonProperty("isBioLoadSafe")
    private boolean bioLoadSafe;
    private List<String> warnings;
    private String recommendedPhRange;
    private String recommendedTempRange;
}
