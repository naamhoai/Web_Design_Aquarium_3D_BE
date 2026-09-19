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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(request.getUserId())
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(request.getUserId()).build()));

        CartItem item = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), request.getProductVariantId())
                .orElseGet(() -> CartItem.builder()
                        .cartId(cart.getId())
                        .productVariantId(request.getProductVariantId())
                        .userDesignId(request.getUserDesignId())
                        .quantity(0)
                        .customConfiguration(request.getCustomConfiguration() != null ? request.getCustomConfiguration() : "{}")
                        .build());

        item.setQuantity(item.getQuantity() + request.getQuantity());
        if (request.getUserDesignId() != null) {
            item.setUserDesignId(request.getUserDesignId());
        }
        if (request.getCustomConfiguration() != null) {
            item.setCustomConfiguration(request.getCustomConfiguration());
        }

        cartItemRepository.save(item);
        log.info("Added {} units of variant {} to cart {} for user {}",
                request.getQuantity(), request.getProductVariantId(), cart.getId(), request.getUserId());

        return getCart(request.getUserId());
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem ci : items) {
            CartItemResponse res = CartItemResponse.fromEntity(ci);

            // Fetch variant, product and supplier information
            String sql = """
                SELECT pv.sku, pv.name as variant_name, pv.price,
                       p.name as product_name, s.id as supplier_id, s.store_name
                FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                JOIN suppliers s ON p.supplier_id = s.id
                WHERE pv.id = ?
            """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ci.getProductVariantId());
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                res.setSku((String) row.get("sku"));
                res.setVariantName((String) row.get("variant_name"));
                res.setProductName((String) row.get("product_name"));
                res.setSupplierId((UUID) row.get("supplier_id"));
                res.setStoreName((String) row.get("store_name"));

                BigDecimal price = (BigDecimal) row.get("price");
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
                res.setPrice(price);
                res.setSubtotal(subtotal);

                totalPrice = totalPrice.add(subtotal);
            }

            totalItems += ci.getQuantity();
            itemResponses.add(res);
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(userId)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .items(itemResponses)
                .build();
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ"));

        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy giỏ hàng"));

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(cart.getUserId());
    }

    @Transactional
    public CartResponse removeItem(UUID cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sản phẩm trong giỏ"));

        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy giỏ hàng"));

        cartItemRepository.delete(item);
        return getCart(cart.getUserId());
    }

    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }
}
