package com.aquarium.order.dto;

import com.aquarium.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private UUID id;
    private UUID subOrderId;
    private UUID productVariantId;
    private String productName;
    private String variantName;
    private String sku;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private Integer layerIndex;
    private String bomParentSku;
    private Boolean isLivestock;
    private Boolean isFragileGlass;

    public static OrderItemResponse fromEntity(OrderItem item) {
        if (item == null) return null;
        return OrderItemResponse.builder()
                .id(item.getId())
                .subOrderId(item.getSubOrderId())
                .productVariantId(item.getProductVariantId())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .sku(item.getSku())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .layerIndex(item.getLayerIndex())
                .bomParentSku(item.getBomParentSku())
                .isLivestock(item.getIsLivestock())
                .isFragileGlass(item.getIsFragileGlass())
                .build();
    }
}
