package com.aquarium.order.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.order.dto.CheckoutRequest;
import com.aquarium.order.dto.QuoteRequest;
import com.aquarium.order.dto.QuoteResponse;
import com.aquarium.order.dto.OrderResponse;
import com.aquarium.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Orders API", description = "Đặt hàng, tự động tách đơn đa nhà cung cấp và lịch sử mua sắm (U20-U27)")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đặt hàng: giá & phí ship do server tính, giữ hàng trong kho, tách đơn theo nhà cung cấp")
    public ApiResponse<OrderResponse> checkout(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success("Đặt hàng thành công, đơn hàng đã được gửi đến các nhà cung cấp liên quan",
                orderService.checkout(user, request));
    }

    @PostMapping("/quote")
    @Operation(summary = "Báo giá giỏ hàng (công khai): giá thật, phí ship, tình trạng hàng — không tạo đơn")
    public ApiResponse<QuoteResponse> quote(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.success(orderService.quote(request.getItems()));
    }

    @GetMapping("/me")
    @Operation(summary = "Lịch sử đơn hàng của tôi (50 đơn gần nhất)")
    public ApiResponse<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(orderService.getOrdersOfUser(user.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết đơn hàng (chủ đơn hoặc admin)")
    public ApiResponse<OrderResponse> getOrderById(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.success(orderService.getOrderById(user, id));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Tra cứu đơn hàng bằng mã đơn (chủ đơn hoặc admin)")
    public ApiResponse<OrderResponse> getOrderByNumber(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @PathVariable String orderNumber) {
        return ApiResponse.success(orderService.getOrderByNumber(user, orderNumber));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: lịch sử đơn hàng của một khách hàng")
    public ApiResponse<List<OrderResponse>> getOrdersByUser(@PathVariable UUID userId) {
        return ApiResponse.success(orderService.getOrdersOfUser(userId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn khi nhà cung cấp chưa đóng gói (U27)")
    public ApiResponse<OrderResponse> cancelOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return ApiResponse.success("Hủy đơn hàng thành công", orderService.cancelOrder(user, id));
    }
}
