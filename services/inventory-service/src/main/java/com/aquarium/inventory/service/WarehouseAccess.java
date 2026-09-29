package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Kiểm tra quyền sở hữu kho (chống IDOR): nhà cung cấp chỉ được thao tác trên kho của chính mình
 * và chỉ khi gian hàng đang ACTIVE. Admin được thao tác mọi kho.
 */
@Component
@RequiredArgsConstructor
public class WarehouseAccess {

    private final JdbcTemplate jdbcTemplate;

    public void assertCanManageWarehouse(AuthenticatedUser user, UUID warehouseId) {
        if (user.isAdmin()) {
            if (!warehouseExists(warehouseId)) {
                throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);
            }
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM warehouses w
                JOIN suppliers s ON s.id = w.supplier_id
                WHERE w.id = ? AND s.user_id = ? AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
                """, Integer.class, warehouseId, user.id());
        if (count == null || count == 0) {
            // Không phân biệt "không tồn tại" và "không có quyền" để không lộ thông tin kho của người khác
            throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }
    }

    /** Chỉ cho nhập kho sản phẩm do chính nhà cung cấp sở hữu kho đó bán. */
    public void assertVariantBelongsToWarehouseSupplier(UUID variantId, UUID warehouseId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM product_variants pv
                JOIN products p ON p.id = pv.product_id
                JOIN warehouses w ON w.supplier_id = p.supplier_id
                WHERE pv.id = ? AND w.id = ?
                """, Integer.class, variantId, warehouseId);
        if (count == null || count == 0) {
            throw new AppException(ErrorCode.VARIANT_NOT_FOUND, "Biến thể sản phẩm không thuộc nhà cung cấp sở hữu kho này");
        }
    }

    /** Danh sách kho mà user được phép xem (null = tất cả, dành cho admin). */
    public List<UUID> manageableWarehouseIds(AuthenticatedUser user) {
        if (user.isAdmin()) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT w.id FROM warehouses w
                JOIN suppliers s ON s.id = w.supplier_id
                WHERE s.user_id = ? AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
                """, (rs, i) -> rs.getObject(1, UUID.class), user.id());
    }

    private boolean warehouseExists(UUID warehouseId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouses WHERE id = ?", Integer.class, warehouseId);
        return count != null && count > 0;
    }
}
