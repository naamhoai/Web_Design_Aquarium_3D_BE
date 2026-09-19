package com.aquarium.order.dto;

import com.aquarium.order.entity.CartItem;
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
public class CartItemResponse {
    private UUID id;
    private UUID cartId;
    private UUID productVariantId;
    private UUID userDesignId;
    private Integer quantity;
    private String customConfiguration;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String productName;
    private String variantName;
    private String sku;
    private UUID supplierId;
    private String storeName;
    private Instant createdAt;

    public static CartItemResponse fromEntity(CartItem item) {
        if (item == null) return null;
        return CartItemResponse.builder()
                .id(item.getId())
                .cartId(item.getCartId())
                .productVariantId(item.getProductVariantId())
                .userDesignId(item.getUserDesignId())
                .quantity(item.getQuantity())
                .customConfiguration(item.getCustomConfiguration())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
