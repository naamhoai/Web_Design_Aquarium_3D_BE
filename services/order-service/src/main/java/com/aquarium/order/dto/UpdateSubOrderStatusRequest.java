package com.aquarium.order.dto;

import com.aquarium.order.entity.SubOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubOrderStatusRequest {

    @NotNull(message = "status is required")
    private SubOrderStatus status;
}
