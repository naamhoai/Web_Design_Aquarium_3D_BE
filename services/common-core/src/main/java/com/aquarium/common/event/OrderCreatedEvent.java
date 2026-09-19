package com.aquarium.common.event;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
    private UUID orderId;
    private String orderNumber;
    private UUID userId;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private List<OrderItemEventPayload> items;
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEventPayload implements Serializable {
        private UUID variantId;
        private String sku;
        private int quantity;
        private UUID warehouseId;
        private boolean isLivestock;
        private Integer layerIndex; // 1-6 for BOM
    }
}
