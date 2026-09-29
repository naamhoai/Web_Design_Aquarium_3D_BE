package com.aquarium.order.service.support;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Giữ / nhả / xuất kho bằng câu lệnh UPDATE có điều kiện (nguyên tử ở mức DB):
 * hai đơn hàng đặt cùng lúc không thể cùng giữ vượt quá tồn kho khả dụng.
 * Các thao tác chạy trong transaction của đơn hàng nên lỗi ở bước sau sẽ tự hoàn tác việc giữ hàng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAllocator {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Giữ {@code quantity} đơn vị tại một kho đang hoạt động của chính nhà cung cấp bán sản phẩm.
     * @return id kho đã giữ hàng
     */
    public UUID reserve(UUID variantId, UUID supplierId, int quantity, String productLabel) {
        List<Map<String, Object>> candidates = jdbcTemplate.queryForList("""
                SELECT i.id, i.warehouse_id, (i.stock_quantity - i.reserved_quantity) AS available
                FROM inventory_items i
                JOIN warehouses w ON w.id = i.warehouse_id
                WHERE i.product_variant_id = ? AND w.supplier_id = ? AND w.is_active = TRUE
                ORDER BY available DESC, w.code
                """, variantId, supplierId);

        int bestAvailable = 0;
        for (Map<String, Object> row : candidates) {
            int available = ((Number) row.get("available")).intValue();
            bestAvailable = Math.max(bestAvailable, available);
            if (available < quantity) {
                continue;
            }
            int updated = jdbcTemplate.update("""
                    UPDATE inventory_items
                    SET reserved_quantity = reserved_quantity + ?
                    WHERE id = ? AND stock_quantity - reserved_quantity >= ?
                    """, quantity, row.get("id"), quantity);
            if (updated == 1) {
                return (UUID) row.get("warehouse_id");
            }
        }
        throw new AppException(ErrorCode.INSUFFICIENT_STOCK, String.format(
                "Sản phẩm \"%s\" không đủ hàng (yêu cầu %d, còn tối đa %d tại một kho)", productLabel, quantity, bestAvailable));
    }

    /**
     * Số lượng tối đa có thể giữ tại MỘT kho đang hoạt động của đúng nhà cung cấp bán sản phẩm
     * (cùng quy tắc với {@link #reserve}) — dùng cho báo giá, không khóa hay thay đổi tồn kho.
     */
    public Map<UUID, Integer> maxAvailableAtSingleWarehouse(Collection<UUID> variantIds) {
        Map<UUID, Integer> result = new HashMap<>();
        if (variantIds.isEmpty()) {
            return result;
        }
        String sql = """
                SELECT i.product_variant_id, MAX(i.stock_quantity - i.reserved_quantity) AS available
                FROM inventory_items i
                JOIN warehouses w ON w.id = i.warehouse_id AND w.is_active = TRUE
                JOIN product_variants pv ON pv.id = i.product_variant_id
                JOIN products p ON p.id = pv.product_id AND p.supplier_id = w.supplier_id
                WHERE i.product_variant_id = ANY(?)
                GROUP BY i.product_variant_id
                """;
        jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setArray(1, connection.createArrayOf("uuid", variantIds.toArray()));
            return ps;
        }, (RowCallbackHandler) rs -> result.put(rs.getObject(1, UUID.class), Math.max(0, rs.getInt(2))));
        return result;
    }

    /** Nhả lượng hàng đã giữ (khi hủy đơn / hủy đơn con). */
    public void release(UUID warehouseId, UUID variantId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_items
                SET reserved_quantity = GREATEST(reserved_quantity - ?, 0)
                WHERE warehouse_id = ? AND product_variant_id = ?
                """, quantity, warehouseId, variantId);
        if (updated == 0) {
            log.warn("Không tìm thấy dòng tồn kho để nhả hàng: warehouse={}, variant={}", warehouseId, variantId);
        }
    }

    /** Chuyển lượng đã giữ thành xuất kho thực tế (khi đơn con chuyển sang SHIPPING) + ghi nhật ký kho. */
    public void shipReserved(UUID warehouseId, UUID variantId, int quantity, UUID orderId, UUID actorUserId, String note) {
        List<UUID> itemIds = jdbcTemplate.query("""
                UPDATE inventory_items
                SET stock_quantity = stock_quantity - ?, reserved_quantity = reserved_quantity - ?
                WHERE warehouse_id = ? AND product_variant_id = ? AND reserved_quantity >= ? AND stock_quantity >= ?
                RETURNING id
                """, (rs, i) -> rs.getObject(1, UUID.class),
                quantity, quantity, warehouseId, variantId, quantity, quantity);
        if (itemIds.isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT, "Tồn kho không khớp với lượng hàng đã giữ cho đơn này");
        }
        jdbcTemplate.update("""
                INSERT INTO inventory_movements (inventory_item_id, movement_type, quantity, reference_order_id, note, created_by)
                VALUES (?, CAST(? AS movement_type_enum), ?, ?, ?, ?)
                """, itemIds.get(0), "EXPORT", quantity, orderId, note, actorUserId);
    }
}
