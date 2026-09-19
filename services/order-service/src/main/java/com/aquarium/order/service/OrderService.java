package com.aquarium.order.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.order.dto.*;
import com.aquarium.order.entity.*;
import com.aquarium.order.repository.CartItemRepository;
import com.aquarium.order.repository.CartRepository;
import com.aquarium.order.repository.OrderItemRepository;
import com.aquarium.order.repository.OrderRepository;
import com.aquarium.order.repository.SubOrderRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final JdbcTemplate jdbcTemplate;

    @Data
    @Builder
    private static class VariantInfo {
        private UUID variantId;
        private String sku;
        private String variantName;
        private BigDecimal price;
        private String productName;
        private Boolean isLivestock;
        private Boolean isFragileGlass;
        private UUID supplierId;
        private String storeName;
        private BigDecimal commissionRate;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Cart cart = cartRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Người dùng chưa khởi tạo giỏ hàng"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Giỏ hàng của bạn đang trống, không thể đặt hàng");
        }

        // 1. Fetch variant and supplier details for all items
        Map<UUID, List<Map.Entry<CartItem, VariantInfo>>> supplierGroupedItems = new LinkedHashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem ci : cartItems) {
            String sql = """
                SELECT pv.id as variant_id, pv.sku, pv.name as variant_name, pv.price,
                       p.name as product_name, p.is_livestock, p.is_fragile_glass,
                       s.id as supplier_id, s.store_name, s.commission_rate
                FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                JOIN suppliers s ON p.supplier_id = s.id
                WHERE pv.id = ?
            """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, ci.getProductVariantId());
            if (rows.isEmpty()) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin sản phẩm: " + ci.getProductVariantId());
            }

            Map<String, Object> r = rows.get(0);
            VariantInfo vi = VariantInfo.builder()
                    .variantId((UUID) r.get("variant_id"))
                    .sku((String) r.get("sku"))
                    .variantName((String) r.get("variant_name"))
                    .price((BigDecimal) r.get("price"))
                    .productName((String) r.get("product_name"))
                    .isLivestock((Boolean) r.get("is_livestock"))
                    .isFragileGlass((Boolean) r.get("is_fragile_glass"))
                    .supplierId((UUID) r.get("supplier_id"))
                    .storeName((String) r.get("store_name"))
                    .commissionRate(r.get("commission_rate") != null ? (BigDecimal) r.get("commission_rate") : BigDecimal.valueOf(8.00))
                    .build();

            BigDecimal subtotal = vi.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            supplierGroupedItems.computeIfAbsent(vi.getSupplierId(), k -> new ArrayList<>())
                    .add(new AbstractMap.SimpleEntry<>(ci, vi));
        }

        // 2. Generate Master Order Number
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "ORD-" + datePrefix + "-" + randomSuffix;

        BigDecimal shippingFee = request.getShippingFee() != null ? request.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount);

        Order masterOrder = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(ensureJson(request.getShippingAddress()))
                .customerNotes(request.getCustomerNotes())
                .build();

        Order savedMasterOrder = orderRepository.save(masterOrder);
        log.info("Created master order {} (ID: {}) for user {}", orderNumber, savedMasterOrder.getId(), request.getUserId());

        // 3. Auto-Split Sub-Orders by Vendor
        int numVendors = supplierGroupedItems.size();
        BigDecimal perVendorShipping = numVendors > 0
                ? shippingFee.divide(BigDecimal.valueOf(numVendors), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SubOrderResponse> subOrderResponses = new ArrayList<>();

        for (Map.Entry<UUID, List<Map.Entry<CartItem, VariantInfo>>> entry : supplierGroupedItems.entrySet()) {
            UUID supplierId = entry.getKey();
            List<Map.Entry<CartItem, VariantInfo>> items = entry.getValue();

            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal commissionRate = items.get(0).getValue().getCommissionRate();

            for (Map.Entry<CartItem, VariantInfo> itemEntry : items) {
                CartItem ci = itemEntry.getKey();
                VariantInfo vi = itemEntry.getValue();
                subtotal = subtotal.add(vi.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            }

            BigDecimal commissionAmount = subtotal.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal payoutAmount = subtotal.subtract(commissionAmount).add(perVendorShipping);

            SubOrder subOrder = SubOrder.builder()
                    .orderId(savedMasterOrder.getId())
                    .supplierId(supplierId)
                    .subtotal(subtotal)
                    .shippingFee(perVendorShipping)
                    .commissionAmount(commissionAmount)
                    .payoutAmount(payoutAmount)
                    .status(SubOrderStatus.PENDING)
                    .build();

            SubOrder savedSubOrder = subOrderRepository.save(subOrder);
            log.info("Auto-split sub-order {} for supplier {} (Subtotal: {}, Commission: {}, Payout: {})",
                    savedSubOrder.getId(), supplierId, subtotal, commissionAmount, payoutAmount);

            List<OrderItemResponse> orderItemResponses = new ArrayList<>();
            for (Map.Entry<CartItem, VariantInfo> itemEntry : items) {
                CartItem ci = itemEntry.getKey();
                VariantInfo vi = itemEntry.getValue();
                BigDecimal itemSubtotal = vi.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));

                OrderItem orderItem = OrderItem.builder()
                        .subOrderId(savedSubOrder.getId())
                        .productVariantId(vi.getVariantId())
                        .productName(vi.getProductName())
                        .variantName(vi.getVariantName())
                        .sku(vi.getSku())
                        .price(vi.getPrice())
                        .quantity(ci.getQuantity())
                        .subtotal(itemSubtotal)
                        .isLivestock(vi.getIsLivestock())
                        .isFragileGlass(vi.getIsFragileGlass())
                        .build();

                OrderItem savedOrderItem = orderItemRepository.save(orderItem);
                orderItemResponses.add(OrderItemResponse.fromEntity(savedOrderItem));
            }

            subOrderResponses.add(SubOrderResponse.fromEntity(savedSubOrder, orderItemResponses));
        }

        // 4. Clear cart after successful checkout
        cartItemRepository.deleteByCartId(cart.getId());
        log.info("Cleared cart for user {} after successful order placement", request.getUserId());

        return OrderResponse.fromEntity(savedMasterOrder, subOrderResponses);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng: " + orderId));

        return buildFullOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy mã đơn hàng: " + orderNumber));

        return buildFullOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::buildFullOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubOrderResponse> getSubOrdersBySupplier(UUID supplierId, SubOrderStatus status) {
        List<SubOrder> subOrders = (status != null)
                ? subOrderRepository.findBySupplierIdAndStatusOrderByCreatedAtDesc(supplierId, status)
                : subOrderRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);

        return subOrders.stream()
                .map(so -> {
                    List<OrderItemResponse> items = orderItemRepository.findBySubOrderId(so.getId()).stream()
                            .map(OrderItemResponse::fromEntity)
                            .collect(Collectors.toList());
                    return SubOrderResponse.fromEntity(so, items);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public SubOrderResponse updateSubOrderStatus(UUID subOrderId, SubOrderStatus newStatus) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng con"));

        subOrder.setStatus(newStatus);
        SubOrder saved = subOrderRepository.save(subOrder);
        log.info("Updated sub-order {} status to {}", subOrderId, newStatus);

        List<OrderItemResponse> items = orderItemRepository.findBySubOrderId(saved.getId()).stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());

        return SubOrderResponse.fromEntity(saved, items);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL, "Đơn hàng đã được xuất kho vận chuyển, không thể hủy");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Cancel all associated sub-orders
        List<SubOrder> subOrders = subOrderRepository.findByOrderId(orderId);
        for (SubOrder so : subOrders) {
            so.setStatus(SubOrderStatus.CANCELLED);
            subOrderRepository.save(so);
        }

        return buildFullOrderResponse(saved);
    }

    private OrderResponse buildFullOrderResponse(Order order) {
        List<SubOrder> subOrders = subOrderRepository.findByOrderId(order.getId());
        List<SubOrderResponse> subOrderResponses = subOrders.stream()
                .map(so -> {
                    List<OrderItemResponse> items = orderItemRepository.findBySubOrderId(so.getId()).stream()
                            .map(OrderItemResponse::fromEntity)
                            .collect(Collectors.toList());
                    return SubOrderResponse.fromEntity(so, items);
                })
                .collect(Collectors.toList());

        return OrderResponse.fromEntity(order, subOrderResponses);
    }

    private String ensureJson(String input) {
        if (input == null || input.isBlank()) {
            return "{}";
        }
        String trimmed = input.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(java.util.Map.of("address", trimmed));
        } catch (Exception e) {
            return "{\"address\": \"" + trimmed.replace("\"", "\\\"") + "\"}";
        }
    }
}
