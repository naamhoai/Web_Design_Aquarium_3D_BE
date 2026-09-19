package com.aquarium.supplier.dto;

import com.aquarium.supplier.entity.Supplier;
import com.aquarium.supplier.entity.SupplierStatus;
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
public class SupplierResponse {
    private UUID id;
    private UUID userId;
    private String storeName;
    private String slug;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String businessLicense;
    private String taxCode;
    private BigDecimal rating;
    private Integer reviewCount;
    private BigDecimal commissionRate;
    private SupplierStatus status;
    private Instant createdAt;

    public static SupplierResponse fromEntity(Supplier supplier) {
        if (supplier == null) return null;
        return SupplierResponse.builder()
                .id(supplier.getId())
                .userId(supplier.getUserId())
                .storeName(supplier.getStoreName())
                .slug(supplier.getSlug())
                .description(supplier.getDescription())
                .logoUrl(supplier.getLogoUrl())
                .bannerUrl(supplier.getBannerUrl())
                .businessLicense(supplier.getBusinessLicense())
                .taxCode(supplier.getTaxCode())
                .rating(supplier.getRating())
                .reviewCount(supplier.getReviewCount())
                .commissionRate(supplier.getCommissionRate())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}
