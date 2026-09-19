package com.aquarium.order.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.order.dto.AddToCartRequest;
import com.aquarium.order.dto.CartResponse;
import com.aquarium.order.dto.UpdateCartItemQuantityRequest;
import com.aquarium.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart API", description = "Quản lý giỏ hàng thông minh, hỗ trợ lưu cấu hình tùy biến bể cá 3D (U15-U19)")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm hoặc combo bể 3D tùy biến vào giỏ hàng (U15-U16)")
    public ApiResponse<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        CartResponse response = cartService.addToCart(request);
        return ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng thành công", response);
    }

    @GetMapping
    @Operation(summary = "Xem chi tiết toàn bộ giỏ hàng của người dùng (U17)")
    public ApiResponse<CartResponse> getCart(@RequestParam UUID userId) {
        CartResponse response = cartService.getCart(userId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/items/{cartItemId}")
    @Operation(summary = "Thay đổi số lượng mặt hàng trong giỏ (U18)")
    public ApiResponse<CartResponse> updateQuantity(
            @PathVariable UUID cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        CartResponse response = cartService.updateItemQuantity(cartItemId, request.getQuantity());
        return ApiResponse.success("Cập nhật số lượng thành công", response);
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Xóa một mặt hàng khỏi giỏ (U18)")
    public ApiResponse<CartResponse> removeItem(@PathVariable UUID cartItemId) {
        CartResponse response = cartService.removeItem(cartItemId);
        return ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", response);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Làm trống toàn bộ giỏ hàng (U19)")
    public ApiResponse<Void> clearCart(@RequestParam UUID userId) {
        cartService.clearCart(userId);
        return ApiResponse.success("Đã làm trống giỏ hàng", null);
    }
}
