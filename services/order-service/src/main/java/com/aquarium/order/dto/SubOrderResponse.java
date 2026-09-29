package com.aquarium.order.dto;

import com.aquarium.order.entity.SubOrder;
import com.aquarium.order.entity.SubOrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubOrderResponse {
    private UUID id;
    private UUID orderId;
    private UUID supplierId;
    private String storeName;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal commissionAmount;
    private BigDecimal payoutAmount;
    private SubOrderStatus status;
    private Instant createdAt;
    private List<OrderItemResponse> items;

    /** Chỉ điền cho nhà cung cấp/admin để giao hàng. */
    private String orderNumber;
    private String shippingAddress;
    private String customerNotes;

    public static SubOrderResponse fromEntity(SubOrder subOrder, List<OrderItemResponse> items) {
        if (subOrder == null) return null;
        return SubOrderResponse.builder()
                .id(subOrder.getId())
                .orderId(subOrder.getOrderId())
                .supplierId(subOrder.getSupplierId())
                .subtotal(subOrder.getSubtotal())
                .shippingFee(subOrder.getShippingFee())
                .commissionAmount(subOrder.getCommissionAmount())
                .payoutAmount(subOrder.getPayoutAmount())
                .status(subOrder.getStatus())
                .createdAt(subOrder.getCreatedAt())
                .items(items)
                .build();
    }
}
