package com.aquarium.order.dto;

import com.aquarium.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CheckoutRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "paymentMethod is required")
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @NotBlank(message = "shippingAddress is required")
    private String shippingAddress;

    private String customerNotes;

    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.valueOf(50000);

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
}
