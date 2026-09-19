package com.aquarium.catalog.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private UUID id;
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer weightGrams;
    private String dimensionsCm;
    private String attributes;
}
