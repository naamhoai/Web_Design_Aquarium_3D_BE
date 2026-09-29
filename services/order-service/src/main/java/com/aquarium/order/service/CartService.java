package com.aquarium.order.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.order.dto.AddToCartRequest;
import com.aquarium.order.dto.CartItemResponse;
import com.aquarium.order.dto.CartResponse;
import com.aquarium.order.entity.Cart;
import com.aquarium.order.entity.CartItem;
import com.aquarium.order.repository.CartItemRepository;
import com.aquarium.order.repository.CartRepository;
import com.aquarium.order.service.support.CatalogLookup;
import com.aquarium.order.service.support.CatalogLookup.VariantInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Giỏ hàng lưu DB. Mọi thao tác đều gắn với user lấy từ JWT — không thể xem/sửa giỏ của người khác.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QTY_PER_LINE = 99;
    private static final int MAX_LINES_PER_CART = 50;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogLookup catalogLookup;
    private final ObjectMapper objectMapper;

    @Transactional
    public CartResponse addToCart(UUID userId, AddToCartRequest request) {
        VariantInfo info = catalogLookup.findByVariantIds(List.of(request.getProductVariantId()))
                .get(request.getProductVariantId());
        if (info == null || !info.purchasable()) {
            throw new AppException(ErrorCode.VARIANT_NOT_FOUND, "Sản phẩm không tồn tại hoặc đã ngừng bán");
        }
        if (request.getUserDesignId() != null) {
            UUID owner = catalogLookup.findDesignOwner(request.getUserDesignId());
            if (owner == null || !owner.equals(userId)) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bản thiết kế 3D");
            }
        }
        String configuration = normalizeJson(request.getCustomConfiguration());

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.saveAndFlush(Cart.builder().userId(userId).build()));

        CartItem item = (request.getUserDesignId() != null
                ? cartItemRepository.findByCartIdAndProductVariantIdAndUserDesignId(cart.getId(), request.getProductVariantId(), request.getUserDesignId())
                : cartItemRepository.findByCartIdAndProductVariantIdAndUserDesignIdIsNull(cart.getId(), request.getProductVariantId()))
                .orElse(null);

        if (item == null) {
            if (cartItemRepository.countByCartId(cart.getId()) >= MAX_LINES_PER_CART) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Giỏ hàng tối đa " + MAX_LINES_PER_CART + " sản phẩm");
            }
            item = CartItem.builder()
                    .cartId(cart.getId())
                    .productVariantId(request.getProductVariantId())
                    .userDesignId(request.getUserDesignId())
                    .quantity(0)
                    .customConfiguration(configuration != null ? configuration : "{}")
                    .build();
        } else if (configuration != null) {
            item.setCustomConfiguration(configuration);
        }

        int newQuantity = item.getQuantity() + request.getQuantity();
        if (newQuantity > MAX_QTY_PER_LINE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng mỗi sản phẩm tối đa " + MAX_QTY_PER_LINE);
        }
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return buildCart(userId, cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        // Không tạo giỏ khi chỉ đọc (trước đây gọi save() trong transaction read-only → giỏ "ma")
        return cartRepository.findByUserId(userId)
                .map(cart -> buildCart(userId, cart))
                .orElseGet(() -> CartResponse.builder()
                        .userId(userId)
                        .totalItems(0)
                        .totalPrice(BigDecimal.ZERO)
                        .items(List.of())
                        .build());
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID cartItemId, int quantity) {
        Cart cart = requireCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ"));
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return buildCart(userId, cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID cartItemId) {
        Cart cart = requireCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ"));
        cartItemRepository.delete(item);
        return buildCart(userId, cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    private Cart requireCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ"));
    }

    private CartResponse buildCart(UUID userId, Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        Map<UUID, VariantInfo> infos = catalogLookup.findByVariantIds(items.stream().map(CartItem::getProductVariantId).toList());

        List<CartItemResponse> responses = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;
        for (CartItem cartItem : items) {
            CartItemResponse response = CartItemResponse.fromEntity(cartItem);
            VariantInfo info = infos.get(cartItem.getProductVariantId());
            if (info != null) {
                BigDecimal subtotal = info.price().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                response.setSku(info.sku());
                response.setVariantName(info.variantName());
                response.setProductName(info.productName());
                response.setSupplierId(info.supplierId());
                response.setStoreName(info.storeName());
                response.setPrice(info.price());
                response.setSubtotal(subtotal);
                if (info.purchasable()) {
                    totalPrice = totalPrice.add(subtotal);
                }
            }
            totalItems += cartItem.getQuantity();
            responses.add(response);
        }
        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(userId)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .items(responses)
                .build();
    }

    /** Chỉ chấp nhận JSON hợp lệ cho cấu hình tùy biến (tránh lưu dữ liệu rác / lỗi 500 từ PostgreSQL). */
    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node == null || !(node.isObject() || node.isArray())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "customConfiguration phải là JSON object/array");
            }
            return objectMapper.writeValueAsString(node);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "customConfiguration không phải JSON hợp lệ");
        }
    }
}
