package com.aquarium.order.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.order.dto.SubOrderResponse;
import com.aquarium.order.dto.UpdateSubOrderStatusRequest;
import com.aquarium.order.entity.SubOrderStatus;
import com.aquarium.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sub-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
@Tag(name = "Supplier Sub-Orders API", description = "Đơn hàng con của từng nhà cung cấp: xác nhận, đóng gói, giao hàng (S18-S27)")
public class SubOrderController {

    private final OrderService orderService;

    @GetMapping("/mine")
    @Operation(summary = "Nhà cung cấp xem đơn hàng con của cửa hàng mình")
    public ApiResponse<List<SubOrderResponse>> getMySubOrders(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @RequestParam(required = false) SubOrderStatus status) {
        return ApiResponse.success(orderService.getSubOrdersForSupplier(user, null, status));
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Xem đơn hàng con theo nhà cung cấp (admin, hoặc chính chủ shop)")
    public ApiResponse<List<SubOrderResponse>> getSubOrdersBySupplier(@AuthenticationPrincipal AuthenticatedUser user,
                                                                      @PathVariable UUID supplierId,
                                                                      @RequestParam(required = false) SubOrderStatus status) {
        return ApiResponse.success(orderService.getSubOrdersForSupplier(user, supplierId, status));
    }

    @PatchMapping("/{subOrderId}/status")
    @Operation(summary = "Cập nhật tiến độ: PENDING→CONFIRMED→PACKING→SHIPPING→DELIVERED (có kiểm tra luồng trạng thái)")
    public ApiResponse<SubOrderResponse> updateSubOrderStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @PathVariable UUID subOrderId,
                                                              @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        return ApiResponse.success("Cập nhật trạng thái đơn hàng con thành công",
                orderService.updateSubOrderStatus(user, subOrderId, request.getStatus()));
    }
}
