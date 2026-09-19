package com.aquarium.order.dto;

import com.aquarium.order.entity.Order;
import com.aquarium.order.entity.OrderStatus;
import com.aquarium.order.entity.PaymentMethod;
import com.aquarium.order.entity.PaymentStatus;
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
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID userId;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private String customerNotes;
    private Instant createdAt;
    private List<SubOrderResponse> subOrders;

    public static OrderResponse fromEntity(Order order, List<SubOrderResponse> subOrders) {
        if (order == null) return null;
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .shippingAddress(order.getShippingAddress())
                .customerNotes(order.getCustomerNotes())
                .createdAt(order.getCreatedAt())
                .subOrders(subOrders)
                .build();
    }
}
