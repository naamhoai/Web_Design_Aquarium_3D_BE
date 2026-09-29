package com.aquarium.order.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * Tra cứu giá & thông tin sản phẩm trực tiếp từ DB. Giá LUÔN lấy từ đây,
 * không bao giờ tin giá do client gửi lên.
 */
@Component
@RequiredArgsConstructor
public class CatalogLookup {

    private static final BigDecimal DEFAULT_COMMISSION = new BigDecimal("8.00");

    public record VariantInfo(UUID variantId, String sku, String variantName, BigDecimal price,
                              UUID productId, String productSku, String productName,
                              boolean livestock, boolean fragileGlass,
                              UUID supplierId, String storeName, BigDecimal commissionRate,
                              boolean purchasable) {
    }

    private static final String PURCHASABLE = "(pv.is_active AND p.status = 'ACTIVE' AND p.deleted_at IS NULL "
            + "AND s.status = 'ACTIVE' AND s.deleted_at IS NULL)";

    private static final String SELECT = """
            SELECT pv.id AS variant_id, pv.sku, pv.name AS variant_name, pv.price,
                   p.id AS product_id, p.sku AS product_sku, p.name AS product_name,
                   COALESCE(p.is_livestock, FALSE) AS is_livestock, COALESCE(p.is_fragile_glass, FALSE) AS is_fragile_glass,
                   s.id AS supplier_id, s.store_name, s.commission_rate,
                   %s AS purchasable
            FROM product_variants pv
            JOIN products p ON p.id = pv.product_id
            JOIN suppliers s ON s.id = p.supplier_id
            """.formatted(PURCHASABLE);

    private static final RowMapper<VariantInfo> ROW_MAPPER = (rs, rowNum) -> new VariantInfo(
            rs.getObject("variant_id", UUID.class),
            rs.getString("sku"),
            rs.getString("variant_name"),
            rs.getBigDecimal("price"),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_sku"),
            rs.getString("product_name"),
            rs.getBoolean("is_livestock"),
            rs.getBoolean("is_fragile_glass"),
            rs.getObject("supplier_id", UUID.class),
            rs.getString("store_name"),
            rs.getBigDecimal("commission_rate") != null ? rs.getBigDecimal("commission_rate") : DEFAULT_COMMISSION,
            rs.getBoolean("purchasable"));

    private final JdbcTemplate jdbcTemplate;

    /**
     * Chuyển danh sách SKU thành biến thể có thể bán. Ưu tiên khớp SKU biến thể;
     * nếu là SKU sản phẩm thì chọn biến thể mặc định (tạo sớm nhất, rồi theo SKU).
     * SKU không tồn tại / ngừng bán sẽ không có trong kết quả.
     */
    public Map<String, VariantInfo> resolvePurchasableSkus(Collection<String> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        Set<String> wanted = new HashSet<>(skus);
        String sql = SELECT + " WHERE " + PURCHASABLE + " AND (pv.sku = ANY(?) OR p.sku = ANY(?)) ORDER BY pv.created_at, pv.sku";
        List<VariantInfo> rows = jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            Array array = connection.createArrayOf("varchar", wanted.toArray());
            ps.setArray(1, array);
            ps.setArray(2, array);
            return ps;
        }, ROW_MAPPER);

        Map<String, VariantInfo> result = new HashMap<>();
        for (VariantInfo v : rows) {
            if (wanted.contains(v.sku())) {
                result.putIfAbsent(v.sku(), v);
            }
        }
        for (VariantInfo v : rows) {
            if (wanted.contains(v.productSku())) {
                result.putIfAbsent(v.productSku(), v);
            }
        }
        return result;
    }

    /** Thông tin biến thể theo id (kể cả đã ngừng bán — dùng để hiển thị giỏ hàng). */
    public Map<UUID, VariantInfo> findByVariantIds(Collection<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        String sql = SELECT + " WHERE pv.id = ANY(?)";
        List<VariantInfo> rows = jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setArray(1, connection.createArrayOf("uuid", new LinkedHashSet<>(variantIds).toArray()));
            return ps;
        }, ROW_MAPPER);
        Map<UUID, VariantInfo> result = new HashMap<>();
        rows.forEach(v -> result.put(v.variantId(), v));
        return result;
    }

    /** Chủ sở hữu của một bản thiết kế 3D (null nếu không tồn tại). */
    public UUID findDesignOwner(UUID designId) {
        List<UUID> owners = jdbcTemplate.query("SELECT user_id FROM user_designs WHERE id = ?",
                (rs, i) -> rs.getObject(1, UUID.class), designId);
        return owners.isEmpty() ? null : owners.get(0);
    }
}
