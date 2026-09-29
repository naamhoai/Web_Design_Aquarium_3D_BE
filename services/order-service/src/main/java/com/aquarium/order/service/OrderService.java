package com.aquarium.order.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.order.dto.*;
import com.aquarium.order.entity.*;
import com.aquarium.order.repository.*;
import com.aquarium.order.service.support.CatalogLookup;
import com.aquarium.order.service.support.CatalogLookup.VariantInfo;
import com.aquarium.order.service.support.InventoryAllocator;
import com.aquarium.order.service.support.SupplierAccess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final char[] ORDER_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<PaymentMethod> SUPPORTED_PAYMENT_METHODS = EnumSet.of(PaymentMethod.COD, PaymentMethod.BANK_TRANSFER);
    private static final int MAX_QTY_PER_VARIANT = 99;
    private static final Pattern ORDER_NUMBER_FORMAT = Pattern.compile("^ORD-\\d{8}-[A-Z0-9]{6,8}$");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Chuyển trạng thái đơn con mà nhà cung cấp được phép thực hiện. */
    private static final Map<SubOrderStatus, Set<SubOrderStatus>> SUPPLIER_TRANSITIONS = Map.of(
            SubOrderStatus.PENDING, EnumSet.of(SubOrderStatus.CONFIRMED, SubOrderStatus.CANCELLED),
            SubOrderStatus.CONFIRMED, EnumSet.of(SubOrderStatus.PACKING, SubOrderStatus.CANCELLED),
            SubOrderStatus.PACKING, EnumSet.of(SubOrderStatus.SHIPPING),
            SubOrderStatus.SHIPPING, EnumSet.of(SubOrderStatus.DELIVERED));

    /** Chuyển trạng thái chỉ admin được thực hiện (hoàn tất / tranh chấp). */
    private static final Map<SubOrderStatus, Set<SubOrderStatus>> ADMIN_EXTRA_TRANSITIONS = Map.of(
            SubOrderStatus.DELIVERED, EnumSet.of(SubOrderStatus.COMPLETED, SubOrderStatus.DISPUTED),
            SubOrderStatus.DISPUTED, EnumSet.of(SubOrderStatus.COMPLETED));

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogLookup catalogLookup;
    private final InventoryAllocator inventoryAllocator;
    private final SupplierAccess supplierAccess;
    private final ObjectMapper objectMapper;

    /** Phí vận chuyển do server quyết định — client không thể tự đặt. */
    @Value("${order.shipping.flat-fee:50000}")
    private BigDecimal shippingFlatFee;

    /** Một dòng hàng sau khi đã tra giá từ DB. */
    private static final class OrderLine {
        final VariantInfo info;
        int quantity;
        UUID warehouseId;

        OrderLine(VariantInfo info) {
            this.info = info;
        }

        BigDecimal subtotal() {
            return info.price().multiply(BigDecimal.valueOf(quantity));
        }
    }

    // ------------------------------------------------------------------ Checkout

    @Transactional
    public OrderResponse checkout(AuthenticatedUser user, CheckoutRequest request) {
        if (!SUPPORTED_PAYMENT_METHODS.contains(request.getPaymentMethod())) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Phương thức thanh toán này chưa được hỗ trợ. Vui lòng chọn COD hoặc chuyển khoản ngân hàng");
        }

        boolean fromCart = request.getItems() == null || request.getItems().isEmpty();
        Map<UUID, OrderLine> lines = fromCart ? linesFromCart(user.id()) : linesFromRequest(request.getItems());

        for (OrderLine line : lines.values()) {
            if (line.quantity > MAX_QTY_PER_VARIANT) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Số lượng \"" + line.info.productName() + "\" vượt quá " + MAX_QTY_PER_VARIANT);
            }
        }

        // 1. Giữ hàng trong kho (nguyên tử). Thiếu hàng => ném lỗi, toàn bộ transaction hoàn tác.
        for (OrderLine line : lines.values()) {
            line.warehouseId = inventoryAllocator.reserve(
                    line.info.variantId(), line.info.supplierId(), line.quantity, line.info.productName());
        }

        // 2. Tính tiền hoàn toàn ở server
        Map<UUID, List<OrderLine>> bySupplier = new LinkedHashMap<>();
        lines.values().forEach(line -> bySupplier.computeIfAbsent(line.info.supplierId(), k -> new ArrayList<>()).add(line));

        BigDecimal totalAmount = lines.values().stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = shippingFlatFee.setScale(0, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        Order order = orderRepository.save(Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(user.id())
                .totalAmount(totalAmount)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddress(toShippingJson(request.getShippingAddress()))
                .customerNotes(trimToNull(request.getCustomerNotes()))
                .build());

        // 3. Tách đơn con theo nhà cung cấp; phí ship chia đều, phần dư cộng vào shop đầu tiên
        int vendorCount = bySupplier.size();
        BigDecimal perVendorShipping = shippingFee.divide(BigDecimal.valueOf(vendorCount), 0, RoundingMode.DOWN);
        BigDecimal shippingRemainder = shippingFee.subtract(perVendorShipping.multiply(BigDecimal.valueOf(vendorCount)));

        List<SubOrderResponse> subOrderResponses = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<UUID, List<OrderLine>> entry : bySupplier.entrySet()) {
            List<OrderLine> supplierLines = entry.getValue();
            VariantInfo sample = supplierLines.get(0).info;
            BigDecimal subtotal = supplierLines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal vendorShipping = first ? perVendorShipping.add(shippingRemainder) : perVendorShipping;
            first = false;
            BigDecimal commission = subtotal.multiply(sample.commissionRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal payout = subtotal.subtract(commission).add(vendorShipping);

            SubOrder subOrder = subOrderRepository.save(SubOrder.builder()
                    .orderId(order.getId())
                    .supplierId(entry.getKey())
                    .subtotal(subtotal)
                    .shippingFee(vendorShipping)
                    .commissionAmount(commission)
                    .payoutAmount(payout)
                    .status(SubOrderStatus.PENDING)
                    .build());

            List<OrderItemResponse> itemResponses = new ArrayList<>();
            for (OrderLine line : supplierLines) {
                OrderItem saved = orderItemRepository.save(OrderItem.builder()
                        .subOrderId(subOrder.getId())
                        .productVariantId(line.info.variantId())
                        .productName(line.info.productName())
                        .variantName(line.info.variantName())
                        .sku(line.info.sku())
                        .price(line.info.price())
                        .quantity(line.quantity)
                        .subtotal(line.subtotal())
                        .isLivestock(line.info.livestock())
                        .isFragileGlass(line.info.fragileGlass())
                        .warehouseId(line.warehouseId)
                        .build());
                itemResponses.add(OrderItemResponse.fromEntity(saved));
            }
            SubOrderResponse response = SubOrderResponse.fromEntity(subOrder, itemResponses);
            response.setStoreName(sample.storeName());
            subOrderResponses.add(response);
        }

        if (fromCart) {
            cartRepository.findByUserId(user.id()).ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
        }

        log.info("Tạo đơn {} cho user {}: {} nhà cung cấp, tổng {}", order.getOrderNumber(), user.id(), vendorCount, finalAmount);
        return OrderResponse.fromEntity(order, subOrderResponses);
    }

    // ------------------------------------------------------------------ Báo giá (không tạo đơn)

    /**
     * Tính giá giỏ hàng bằng đúng công thức lúc đặt hàng (giá DB, phí ship cố định chia đều theo shop)
     * nhưng không giữ hàng và không ghi gì vào DB. SKU không bán được sẽ nằm trong {@code unavailableSkus}.
     */
    @Transactional(readOnly = true)
    public QuoteResponse quote(List<CheckoutRequest.CheckoutItem> items) {
        Map<String, Integer> quantityBySku = new LinkedHashMap<>();
        for (CheckoutRequest.CheckoutItem item : items) {
            quantityBySku.merge(item.getSku().trim(), item.getQuantity(), Integer::sum);
        }
        Map<String, VariantInfo> resolved = catalogLookup.resolvePurchasableSkus(quantityBySku.keySet());
        List<String> unavailableSkus = new ArrayList<>();
        Map<UUID, OrderLine> lines = new LinkedHashMap<>();
        quantityBySku.forEach((sku, quantity) -> {
            VariantInfo info = resolved.get(sku);
            if (info == null) {
                unavailableSkus.add(sku);
            } else {
                lines.computeIfAbsent(info.variantId(), id -> new OrderLine(info)).quantity += quantity;
            }
        });
        Map<UUID, Integer> available = inventoryAllocator.maxAvailableAtSingleWarehouse(lines.keySet());

        Map<UUID, List<OrderLine>> bySupplier = new LinkedHashMap<>();
        lines.values().forEach(line -> bySupplier.computeIfAbsent(line.info.supplierId(), k -> new ArrayList<>()).add(line));

        BigDecimal subtotal = lines.values().stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = lines.isEmpty() ? BigDecimal.ZERO : shippingFlatFee.setScale(0, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO;
        int vendorCount = Math.max(1, bySupplier.size());
        BigDecimal perVendorShipping = shippingFee.divide(BigDecimal.valueOf(vendorCount), 0, RoundingMode.DOWN);
        BigDecimal shippingRemainder = shippingFee.subtract(perVendorShipping.multiply(BigDecimal.valueOf(vendorCount)));

        boolean orderable = unavailableSkus.isEmpty() && !lines.isEmpty();
        List<QuoteResponse.Group> groups = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<UUID, List<OrderLine>> entry : bySupplier.entrySet()) {
            List<QuoteResponse.Line> quoteLines = new ArrayList<>();
            for (OrderLine line : entry.getValue()) {
                int availableQty = available.getOrDefault(line.info.variantId(), 0);
                boolean inStock = line.quantity <= MAX_QTY_PER_VARIANT && availableQty >= line.quantity;
                orderable &= inStock;
                quoteLines.add(QuoteResponse.Line.builder()
                        .productVariantId(line.info.variantId())
                        .productId(line.info.productId())
                        .sku(line.info.sku())
                        .productName(line.info.productName())
                        .variantName(line.info.variantName())
                        .price(line.info.price())
                        .quantity(line.quantity)
                        .subtotal(line.subtotal())
                        .livestock(line.info.livestock())
                        .fragileGlass(line.info.fragileGlass())
                        .availableQuantity(availableQty)
                        .inStock(inStock)
                        .build());
            }
            groups.add(QuoteResponse.Group.builder()
                    .supplierId(entry.getKey())
                    .storeName(entry.getValue().get(0).info.storeName())
                    .subtotal(entry.getValue().stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .shippingFee(first ? perVendorShipping.add(shippingRemainder) : perVendorShipping)
                    .items(quoteLines)
                    .build());
            first = false;
        }

        return QuoteResponse.builder()
                .groups(groups)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .total(subtotal.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO))
                .unavailableSkus(unavailableSkus)
                .orderable(orderable)
                .build();
    }

    private Map<UUID, OrderLine> linesFromRequest(List<CheckoutRequest.CheckoutItem> items) {
        Map<String, Integer> quantityBySku = new LinkedHashMap<>();
        for (CheckoutRequest.CheckoutItem item : items) {
            quantityBySku.merge(item.getSku().trim(), item.getQuantity(), Integer::sum);
        }
        Map<String, VariantInfo> resolved = catalogLookup.resolvePurchasableSkus(quantityBySku.keySet());
        List<String> missing = quantityBySku.keySet().stream().filter(sku -> !resolved.containsKey(sku)).toList();
        if (!missing.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                    "Sản phẩm không tồn tại hoặc đã ngừng bán: " + String.join(", ", missing.subList(0, Math.min(5, missing.size()))));
        }
        Map<UUID, OrderLine> lines = new LinkedHashMap<>();
        quantityBySku.forEach((sku, quantity) -> {
            VariantInfo info = resolved.get(sku);
            lines.computeIfAbsent(info.variantId(), id -> new OrderLine(info)).quantity += quantity;
        });
        return lines;
    }

    private Map<UUID, OrderLine> linesFromCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Giỏ hàng của bạn đang trống, không thể đặt hàng"));
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Giỏ hàng của bạn đang trống, không thể đặt hàng");
        }
        Map<UUID, VariantInfo> infos = catalogLookup.findByVariantIds(
                cartItems.stream().map(CartItem::getProductVariantId).toList());
        Map<UUID, OrderLine> lines = new LinkedHashMap<>();
        for (CartItem cartItem : cartItems) {
            VariantInfo info = infos.get(cartItem.getProductVariantId());
            if (info == null || !info.purchasable()) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Một sản phẩm trong giỏ đã ngừng bán, vui lòng xóa khỏi giỏ trước khi đặt hàng");
            }
            lines.computeIfAbsent(info.variantId(), id -> new OrderLine(info)).quantity += cartItem.getQuantity();
        }
        return lines;
    }

    // ------------------------------------------------------------------ Tra cứu đơn hàng

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(AuthenticatedUser user, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> canView(user, o))
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return buildFullOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(AuthenticatedUser user, String orderNumber) {
        if (orderNumber == null || !ORDER_NUMBER_FORMAT.matcher(orderNumber).matches()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .filter(o -> canView(user, o))
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return buildFullOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersOfUser(UUID userId) {
        return orderRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::buildFullOrderResponse)
                .collect(Collectors.toList());
    }

    /** Chủ đơn hoặc admin mới xem được; người khác nhận 404 để không lộ sự tồn tại của đơn (chống IDOR). */
    private static boolean canView(AuthenticatedUser user, Order order) {
        return user.isAdmin() || order.getUserId().equals(user.id());
    }

    // ------------------------------------------------------------------ Hủy đơn

    @Transactional
    public OrderResponse cancelOrder(AuthenticatedUser user, UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(o -> canView(user, o))
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.PROCESSING).contains(order.getStatus())) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL,
                    "Đơn hàng đang ở trạng thái " + order.getStatus() + ", không thể hủy");
        }
        List<SubOrder> subOrders = subOrderRepository.findByOrderId(orderId);
        boolean alreadyInFulfillment = subOrders.stream().anyMatch(s -> !EnumSet.of(
                SubOrderStatus.PENDING, SubOrderStatus.CONFIRMED, SubOrderStatus.CANCELLED).contains(s.getStatus()));
        if (alreadyInFulfillment) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL, "Đơn hàng đã được đóng gói hoặc bàn giao vận chuyển, không thể hủy");
        }

        for (SubOrder subOrder : subOrders) {
            if (subOrder.getStatus() != SubOrderStatus.CANCELLED) {
                releaseReservations(orderItemRepository.findBySubOrderId(subOrder.getId()));
                subOrder.setStatus(SubOrderStatus.CANCELLED);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        log.info("Đơn {} đã bị hủy bởi user {}", order.getOrderNumber(), user.id());
        return buildFullOrderResponse(order);
    }

    // ------------------------------------------------------------------ Đơn con (nhà cung cấp)

    @Transactional(readOnly = true)
    public List<SubOrderResponse> getSubOrdersForSupplier(AuthenticatedUser user, UUID supplierId, SubOrderStatus status) {
        UUID effectiveSupplierId = resolveSupplierScope(user, supplierId);
        List<SubOrder> subOrders = status != null
                ? subOrderRepository.findBySupplierIdAndStatusOrderByCreatedAtDesc(effectiveSupplierId, status)
                : subOrderRepository.findBySupplierIdOrderByCreatedAtDesc(effectiveSupplierId);
        Map<UUID, String> storeNames = supplierAccess.storeNames(List.of(effectiveSupplierId));
        return subOrders.stream().map(subOrder -> {
            Order order = orderRepository.findById(subOrder.getOrderId()).orElse(null);
            return fulfillmentView(subOrder, order, storeNames);
        }).collect(Collectors.toList());
    }

    @Transactional
    public SubOrderResponse updateSubOrderStatus(AuthenticatedUser user, UUID subOrderId, SubOrderStatus target) {
        UUID orderId = subOrderRepository.findOrderIdById(subOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng con"));

        // Khóa theo thứ tự cố định (đơn cha trước, đơn con sau) để tránh deadlock với luồng hủy đơn
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        SubOrder subOrder = subOrderRepository.findByIdForUpdate(subOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy đơn hàng con"));
        assertCanManage(user, subOrder.getSupplierId());

        SubOrderStatus current = subOrder.getStatus();
        if (order.getStatus() == OrderStatus.CANCELLED || !isTransitionAllowed(current, target, user.isAdmin())) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể chuyển đơn hàng con từ " + current + " sang " + target);
        }

        List<OrderItem> items = orderItemRepository.findBySubOrderId(subOrder.getId());
        if (target == SubOrderStatus.CANCELLED) {
            releaseReservations(items);
        } else if (target == SubOrderStatus.SHIPPING) {
            for (OrderItem item : items) {
                if (item.getWarehouseId() != null) {
                    inventoryAllocator.shipReserved(item.getWarehouseId(), item.getProductVariantId(), item.getQuantity(),
                            order.getId(), user.id(), "Xuất kho giao đơn " + order.getOrderNumber());
                }
            }
        }

        subOrder.setStatus(target);
        recomputeOrderStatus(order);
        log.info("Đơn con {} ({}) chuyển {} -> {} bởi {}", subOrderId, order.getOrderNumber(), current, target, user.id());
        return fulfillmentView(subOrder, order, supplierAccess.storeNames(List.of(subOrder.getSupplierId())));
    }

    private UUID resolveSupplierScope(AuthenticatedUser user, UUID requestedSupplierId) {
        if (user.isAdmin()) {
            if (requestedSupplierId == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Thiếu supplierId");
            }
            return requestedSupplierId;
        }
        UUID own = supplierAccess.findActiveSupplierIdByUser(user.id())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Bạn không phải nhà cung cấp đang hoạt động"));
        if (requestedSupplierId != null && !requestedSupplierId.equals(own)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return own;
    }

    private void assertCanManage(AuthenticatedUser user, UUID supplierId) {
        if (user.isAdmin()) {
            return;
        }
        UUID own = supplierAccess.findActiveSupplierIdByUser(user.id()).orElse(null);
        if (own == null || !own.equals(supplierId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private static boolean isTransitionAllowed(SubOrderStatus from, SubOrderStatus to, boolean admin) {
        if (SUPPLIER_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            return true;
        }
        return admin && ADMIN_EXTRA_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    private void recomputeOrderStatus(Order order) {
        List<SubOrderStatus> active = subOrderRepository.findByOrderId(order.getId()).stream()
                .map(SubOrder::getStatus)
                .filter(s -> s != SubOrderStatus.CANCELLED)
                .toList();
        Set<SubOrderStatus> delivered = EnumSet.of(SubOrderStatus.DELIVERED, SubOrderStatus.COMPLETED, SubOrderStatus.DISPUTED);
        OrderStatus next;
        if (active.isEmpty()) {
            next = OrderStatus.CANCELLED;
        } else if (active.stream().allMatch(s -> s == SubOrderStatus.COMPLETED)) {
            next = OrderStatus.COMPLETED;
        } else if (active.stream().allMatch(delivered::contains)) {
            next = OrderStatus.DELIVERED;
        } else if (active.stream().anyMatch(s -> s == SubOrderStatus.SHIPPING || delivered.contains(s))) {
            next = OrderStatus.SHIPPED;
        } else if (active.stream().anyMatch(s -> s == SubOrderStatus.CONFIRMED || s == SubOrderStatus.PACKING)) {
            next = OrderStatus.PROCESSING;
        } else {
            next = order.getStatus();
        }
        order.setStatus(next);
    }

    private void releaseReservations(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getWarehouseId() != null) {
                inventoryAllocator.release(item.getWarehouseId(), item.getProductVariantId(), item.getQuantity());
            }
        }
    }

    // ------------------------------------------------------------------ Mapping

    private OrderResponse buildFullOrderResponse(Order order) {
        List<SubOrder> subOrders = subOrderRepository.findByOrderId(order.getId());
        Map<UUID, String> storeNames = supplierAccess.storeNames(subOrders.stream().map(SubOrder::getSupplierId).toList());
        List<SubOrderResponse> subOrderResponses = subOrders.stream().map(subOrder -> {
            SubOrderResponse response = SubOrderResponse.fromEntity(subOrder, itemsOf(subOrder));
            response.setStoreName(storeNames.get(subOrder.getSupplierId()));
            return response;
        }).collect(Collectors.toList());
        return OrderResponse.fromEntity(order, subOrderResponses);
    }

    private SubOrderResponse fulfillmentView(SubOrder subOrder, Order order, Map<UUID, String> storeNames) {
        SubOrderResponse response = SubOrderResponse.fromEntity(subOrder, itemsOf(subOrder));
        response.setStoreName(storeNames.get(subOrder.getSupplierId()));
        if (order != null) {
            response.setOrderNumber(order.getOrderNumber());
            response.setShippingAddress(order.getShippingAddress());
            response.setCustomerNotes(order.getCustomerNotes());
        }
        return response;
    }

    private List<OrderItemResponse> itemsOf(SubOrder subOrder) {
        return orderItemRepository.findBySubOrderId(subOrder.getId()).stream()
                .map(OrderItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String toShippingJson(CheckoutRequest.ShippingAddressRequest address) {
        Map<String, String> json = new LinkedHashMap<>();
        json.put("recipientName", address.getRecipientName().trim());
        json.put("phone", address.getPhone().trim());
        json.put("addressLine", address.getAddressLine().trim());
        putIfPresent(json, "ward", address.getWard());
        putIfPresent(json, "district", address.getDistrict());
        putIfPresent(json, "city", address.getCity());
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Địa chỉ giao hàng không hợp lệ");
        }
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            map.put(key, trimmed);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDate.now(VN_ZONE).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                code.append(ORDER_CODE_ALPHABET[RANDOM.nextInt(ORDER_CODE_ALPHABET.length)]);
            }
            String candidate = "ORD-" + datePrefix + "-" + code;
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ErrorCode.CONFLICT, "Không thể tạo mã đơn hàng, vui lòng thử lại");
    }
}
