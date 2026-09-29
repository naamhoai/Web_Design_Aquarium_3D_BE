package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.inventory.dto.*;
import com.aquarium.inventory.entity.InventoryItem;
import com.aquarium.inventory.entity.InventoryMovement;
import com.aquarium.inventory.entity.MovementType;
import com.aquarium.inventory.repository.InventoryItemRepository;
import com.aquarium.inventory.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ tồn kho. Mọi thao tác thay đổi số lượng đều:
 * 1) kiểm tra quyền sở hữu kho, 2) khóa dòng tồn kho (SELECT ... FOR UPDATE) rồi mới đọc-kiểm-ghi,
 * nên hai request đồng thời không thể làm tồn kho âm hoặc giữ vượt số lượng thực có.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository movementRepository;
    private final WarehouseAccess warehouseAccess;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public VariantStockSummaryResponse getStockByVariant(UUID productVariantId) {
        List<InventoryItem> items = inventoryItemRepository.findByProductVariantId(productVariantId);

        int totalStock = items.stream().mapToInt(i -> i.getStockQuantity() != null ? i.getStockQuantity() : 0).sum();
        int totalReserved = items.stream().mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();
        int totalAvailable = Math.max(0, totalStock - totalReserved);

        return VariantStockSummaryResponse.builder()
                .productVariantId(productVariantId)
                .totalStock(totalStock)
                .totalReserved(totalReserved)
                .totalAvailable(totalAvailable)
                .inStock(totalAvailable > 0)
                .warehouseBreakdown(items.stream().map(InventoryItemResponse::fromEntity).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getStockByWarehouse(AuthenticatedUser user, UUID warehouseId) {
        warehouseAccess.assertCanManageWarehouse(user, warehouseId);
        return withProductInfo(inventoryItemRepository.findByWarehouseId(warehouseId).stream()
                .map(InventoryItemResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    @Transactional
    public InventoryItemResponse stockIn(AuthenticatedUser user, StockInRequest request) {
        warehouseAccess.assertCanManageWarehouse(user, request.getWarehouseId());
        warehouseAccess.assertVariantBelongsToWarehouseSupplier(request.getProductVariantId(), request.getWarehouseId());

        // Tạo dòng tồn kho nếu chưa có — ON CONFLICT để hai request đồng thời không gây lỗi trùng khóa
        jdbcTemplate.update("""
                INSERT INTO inventory_items (warehouse_id, product_variant_id, stock_quantity, reserved_quantity, low_stock_threshold)
                VALUES (?, ?, 0, 0, 5)
                ON CONFLICT (warehouse_id, product_variant_id) DO NOTHING
                """, request.getWarehouseId(), request.getProductVariantId());

        InventoryItem item = lock(request.getWarehouseId(), request.getProductVariantId());
        item.setStockQuantity(Math.addExact(item.getStockQuantity(), request.getQuantity()));
        InventoryItem saved = inventoryItemRepository.save(item);
        recordMovement(saved.getId(), MovementType.IMPORT, request.getQuantity(), null,
                request.getNote() != null ? request.getNote() : "Nhập kho mới", user.id());

        log.info("User {} nhập {} đơn vị variant {} vào kho {}", user.id(), request.getQuantity(),
                request.getProductVariantId(), request.getWarehouseId());
        return withProductInfo(List.of(InventoryItemResponse.fromEntity(saved))).get(0);
    }

    /** Chỉ admin (thao tác nội bộ). Luồng đặt hàng tự giữ hàng trong order-service. */
    @Transactional
    public InventoryItemResponse reserveStock(ReserveStockRequest request) {
        InventoryItem item = lock(request.getWarehouseId(), request.getProductVariantId());
        if (item.getAvailableQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK, String.format(
                    "Không đủ tồn kho khả dụng để giữ hàng. Khả dụng: %d, Yêu cầu: %d",
                    item.getAvailableQuantity(), request.getQuantity()));
        }
        item.setReservedQuantity(item.getReservedQuantity() + request.getQuantity());
        return InventoryItemResponse.fromEntity(inventoryItemRepository.save(item));
    }

    /** Chỉ admin (thao tác nội bộ). */
    @Transactional
    public InventoryItemResponse releaseStock(ReleaseStockRequest request) {
        InventoryItem item = lock(request.getWarehouseId(), request.getProductVariantId());
        int currentReserved = item.getReservedQuantity() != null ? item.getReservedQuantity() : 0;
        if (currentReserved < request.getQuantity()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng hủy giữ vượt quá số lượng đang bị giữ");
        }
        item.setReservedQuantity(currentReserved - request.getQuantity());
        return InventoryItemResponse.fromEntity(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse stockOut(AuthenticatedUser user, StockOutRequest request) {
        warehouseAccess.assertCanManageWarehouse(user, request.getWarehouseId());
        InventoryItem item = lock(request.getWarehouseId(), request.getProductVariantId());
        int quantity = request.getQuantity();
        int reserved = item.getReservedQuantity() != null ? item.getReservedQuantity() : 0;

        if (request.getReferenceOrderId() != null) {
            // Xuất hàng đã giữ cho một đơn hàng: trừ cả tồn kho và lượng giữ
            if (reserved < quantity || item.getStockQuantity() < quantity) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Lượng hàng đang giữ không đủ để xuất cho đơn này");
            }
            item.setReservedQuantity(reserved - quantity);
        } else if (item.getAvailableQuantity() < quantity) {
            // Xuất tự do chỉ được lấy từ phần khả dụng, không được "ăn" vào hàng đã giữ cho khách khác
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK, String.format(
                    "Tồn kho khả dụng không đủ để xuất (khả dụng %d, yêu cầu %d)", item.getAvailableQuantity(), quantity));
        }
        item.setStockQuantity(item.getStockQuantity() - quantity);
        InventoryItem saved = inventoryItemRepository.save(item);
        recordMovement(saved.getId(), MovementType.EXPORT, quantity, request.getReferenceOrderId(),
                request.getNote() != null ? request.getNote() : "Xuất kho", user.id());

        log.info("User {} xuất {} đơn vị variant {} khỏi kho {} (đơn: {})", user.id(), quantity,
                request.getProductVariantId(), request.getWarehouseId(), request.getReferenceOrderId());
        return withProductInfo(List.of(InventoryItemResponse.fromEntity(saved))).get(0);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getLowStockAlerts(AuthenticatedUser user) {
        List<UUID> warehouseIds = warehouseAccess.manageableWarehouseIds(user);
        List<InventoryItem> items;
        if (warehouseIds == null) {
            items = inventoryItemRepository.findLowStockItems();
        } else if (warehouseIds.isEmpty()) {
            items = List.of();
        } else {
            items = inventoryItemRepository.findLowStockItemsInWarehouses(warehouseIds);
        }
        return withProductInfo(items.stream().map(InventoryItemResponse::fromEntity).collect(Collectors.toList()));
    }

    private InventoryItem lock(UUID warehouseId, UUID variantId) {
        return inventoryItemRepository.lockByWarehouseAndVariant(warehouseId, variantId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private void recordMovement(UUID itemId, MovementType type, int quantity, UUID orderId, String note, UUID actor) {
        movementRepository.save(InventoryMovement.builder()
                .inventoryItemId(itemId)
                .movementType(type)
                .quantity(quantity)
                .referenceOrderId(orderId)
                .note(note)
                .createdBy(actor)
                .build());
    }

    private record VariantLabel(UUID productId, String productName, String variantName, String sku, boolean livestock) {
    }

    /** Gắn tên sản phẩm / SKU (đọc từ bảng sản phẩm dùng chung) để màn hình kho hiển thị được. */
    private List<InventoryItemResponse> withProductInfo(List<InventoryItemResponse> items) {
        Set<UUID> variantIds = items.stream().map(InventoryItemResponse::getProductVariantId).collect(Collectors.toSet());
        if (variantIds.isEmpty()) {
            return items;
        }
        Map<UUID, VariantLabel> labels = new HashMap<>();
        jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    SELECT pv.id, p.id, p.name, pv.name, pv.sku, COALESCE(p.is_livestock, FALSE)
                    FROM product_variants pv
                    JOIN products p ON p.id = pv.product_id
                    WHERE pv.id = ANY(?)
                    """);
            ps.setArray(1, connection.createArrayOf("uuid", variantIds.toArray()));
            return ps;
        }, (RowCallbackHandler) rs -> labels.put(rs.getObject(1, UUID.class), new VariantLabel(
                rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6))));
        for (InventoryItemResponse item : items) {
            VariantLabel label = labels.get(item.getProductVariantId());
            if (label != null) {
                item.setProductId(label.productId());
                item.setProductName(label.productName());
                item.setVariantName(label.variantName());
                item.setSku(label.sku());
                item.setLivestock(label.livestock());
            }
        }
        return items;
    }
}
