package com.aquarium.aquarium3d.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplodedBomResponse {
    private UUID comboProductId;
    private String comboName;
    private String comboSku;
    private BigDecimal totalPrice;
    private List<BomLayerDetailResponse> layers;
}
