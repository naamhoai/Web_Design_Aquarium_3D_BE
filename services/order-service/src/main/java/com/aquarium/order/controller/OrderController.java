package com.aquarium.order.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.order.dto.CheckoutRequest;
import com.aquarium.order.dto.OrderResponse;
import com.aquarium.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders API", description = "Quy trình đặt hàng, tự động tách đơn đa nhà cung cấp và lịch sử mua sắm (U20-U23)")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đặt hàng từ giỏ hàng, tự động bóc tách Master Order & Sub-Orders cho từng Shop (U20)")
    public ApiResponse<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(request);
        return ApiResponse.success("Đặt hàng thành công, đơn hàng đã được gửi đến các nhà cung cấp liên quan", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết đơn hàng theo ID (U21)")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ApiResponse.success(response);
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Tra cứu đơn hàng bằng Mã đơn hàng (VD: ORD-20260917-ABC123)")
    public ApiResponse<OrderResponse> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ApiResponse.success(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy toàn bộ lịch sử đơn hàng của một khách hàng (U22)")
    public ApiResponse<List<OrderResponse>> getOrdersByUser(@PathVariable UUID userId) {
        List<OrderResponse> response = orderService.getOrdersByUser(userId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn hàng khi chưa xuất kho (U23)")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ApiResponse.success("Hủy đơn hàng thành công", response);
    }
}
