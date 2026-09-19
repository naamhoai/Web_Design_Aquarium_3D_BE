package com.aquarium.catalog.dto;

import com.aquarium.catalog.entity.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {
    private UUID id;
    private UUID supplierId;
    private Integer categoryId;
    private String name;
    private String slug;
    private String sku;
    private String shortDescription;
    private BigDecimal basePrice;
    private Boolean isCombo;
    private Boolean is3dCustomizable;
    private Boolean isLivestock;
    private Boolean isFragileGlass;
    private ProductStatus status;
    private Integer totalSales;
    private BigDecimal rating;
    private String thumbnailUrl;
}
