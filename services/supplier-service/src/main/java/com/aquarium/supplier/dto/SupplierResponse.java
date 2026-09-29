package com.aquarium.supplier.dto;

import com.aquarium.supplier.entity.Supplier;
import com.aquarium.supplier.entity.SupplierStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    /** Bản đầy đủ: chỉ dành cho chủ shop và admin. */
    public static SupplierResponse fromEntity(Supplier supplier) {
        return fromEntity(supplier, true);
    }

    /**
     * @param includePrivate false => ẩn thông tin nội bộ (giấy phép, mã số thuế, hoa hồng, userId)
     *                       khi hiển thị trang gian hàng công khai.
     */
    public static SupplierResponse fromEntity(Supplier supplier, boolean includePrivate) {
        if (supplier == null) return null;
        SupplierResponseBuilder builder = SupplierResponse.builder()
                .id(supplier.getId())
                .storeName(supplier.getStoreName())
                .slug(supplier.getSlug())
                .description(supplier.getDescription())
                .logoUrl(supplier.getLogoUrl())
                .bannerUrl(supplier.getBannerUrl())
                .rating(supplier.getRating())
                .reviewCount(supplier.getReviewCount())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt());
        if (includePrivate) {
            builder.userId(supplier.getUserId())
                    .businessLicense(supplier.getBusinessLicense())
                    .taxCode(supplier.getTaxCode())
                    .commissionRate(supplier.getCommissionRate());
        }
        return builder.build();
    }
}
