package com.aquarium.order.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.order.dto.AddToCartRequest;
import com.aquarium.order.dto.CartResponse;
import com.aquarium.order.dto.UpdateCartItemQuantityRequest;
import com.aquarium.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Cart API", description = "Giỏ hàng của người dùng đang đăng nhập (U15-U19)")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm hoặc combo bể 3D tùy biến vào giỏ hàng (U15-U16)")
    public ApiResponse<CartResponse> addToCart(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody AddToCartRequest request) {
        return ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng thành công", cartService.addToCart(user.id(), request));
    }

    @GetMapping
    @Operation(summary = "Xem giỏ hàng của tôi (U17)")
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(cartService.getCart(user.id()));
    }

    @PatchMapping("/items/{cartItemId}")
    @Operation(summary = "Thay đổi số lượng mặt hàng trong giỏ (U18)")
    public ApiResponse<CartResponse> updateQuantity(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable UUID cartItemId,
                                                    @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ApiResponse.success("Cập nhật số lượng thành công",
                cartService.updateItemQuantity(user.id(), cartItemId, request.getQuantity()));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Xóa một mặt hàng khỏi giỏ (U18)")
    public ApiResponse<CartResponse> removeItem(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable UUID cartItemId) {
        return ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", cartService.removeItem(user.id(), cartItemId));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Làm trống giỏ hàng (U19)")
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal AuthenticatedUser user) {
        cartService.clearCart(user.id());
        return ApiResponse.success("Đã làm trống giỏ hàng", null);
    }
}
