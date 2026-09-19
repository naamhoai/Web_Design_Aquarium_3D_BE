package com.aquarium.order.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.order.dto.SubOrderResponse;
import com.aquarium.order.dto.UpdateSubOrderStatusRequest;
import com.aquarium.order.entity.SubOrderStatus;
import com.aquarium.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sub-orders")
@RequiredArgsConstructor
@Tag(name = "Supplier Sub-Orders API", description = "Quản lý đơn hàng con theo từng nhà cung cấp, đóng gói và vận chuyển (S25-S27)")
public class SubOrderController {

    private final OrderService orderService;

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Nhà cung cấp tra cứu danh sách đơn hàng con của cửa hàng mình (S25)")
    public ApiResponse<List<SubOrderResponse>> getSubOrdersBySupplier(
            @PathVariable UUID supplierId,
            @RequestParam(required = false) SubOrderStatus status) {
        List<SubOrderResponse> response = orderService.getSubOrdersBySupplier(supplierId, status);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{subOrderId}/status")
    @Operation(summary = "Cập nhật tiến độ xử lý đơn hàng con: CONFIRMED -> PACKING -> SHIPPING -> DELIVERED (S26)")
    public ApiResponse<SubOrderResponse> updateSubOrderStatus(
            @PathVariable UUID subOrderId,
            @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        SubOrderResponse response = orderService.updateSubOrderStatus(subOrderId, request.getStatus());
        return ApiResponse.success("Cập nhật trạng thái đơn hàng con thành công", response);
    }
}
