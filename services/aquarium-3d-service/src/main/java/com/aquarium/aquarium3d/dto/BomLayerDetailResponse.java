package com.aquarium.aquarium3d.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomLayerDetailResponse {
    private Integer layerIndex; // 1 to 6
    private String layerName;
    private UUID componentVariantId;
    private String componentName;
    private String sku;
    private BigDecimal price;
    private Boolean isRequired;
    private Boolean canSwap;
    private String asset3dUrl;
    private String boundingBox;
}
