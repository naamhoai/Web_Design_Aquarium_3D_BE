package com.aquarium.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Thêm vào giỏ hàng. Chủ giỏ hàng lấy từ JWT, không nhận userId từ client. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {

    @NotNull(message = "productVariantId is required")
    private UUID productVariantId;

    private UUID userDesignId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 99, message = "quantity must be at most 99")
    @Builder.Default
    private Integer quantity = 1;

    @Size(max = 10000, message = "customConfiguration is too large")
    private String customConfiguration;
}
