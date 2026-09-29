package com.aquarium.aquarium3d.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * Tính giá linh kiện của bản thiết kế từ bảng product_variants (nguồn giá duy nhất).
 * Quy tắc chọn biến thể giống order-service: ưu tiên SKU biến thể, SKU sản phẩm → biến thể mặc định.
 */
@Component
@RequiredArgsConstructor
public class DesignPricingService {

    public record PricedComponent(String sku, String productName, String variantName, BigDecimal unitPrice) {
    }

    private final JdbcTemplate jdbcTemplate;

    public Map<String, PricedComponent> resolve(Collection<String> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        Set<String> wanted = new HashSet<>(skus);
        String sql = """
                SELECT pv.sku, p.sku AS product_sku, p.name AS product_name, pv.name AS variant_name, pv.price
                FROM product_variants pv
                JOIN products p ON p.id = pv.product_id
                JOIN suppliers s ON s.id = p.supplier_id
                WHERE pv.is_active AND p.status = 'ACTIVE' AND p.deleted_at IS NULL
                  AND s.status = 'ACTIVE' AND s.deleted_at IS NULL
                  AND (pv.sku = ANY(?) OR p.sku = ANY(?))
                ORDER BY pv.created_at, pv.sku
                """;
        List<Object[]> rows = jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            Array array = connection.createArrayOf("varchar", wanted.toArray());
            ps.setArray(1, array);
            ps.setArray(2, array);
            return ps;
        }, (rs, i) -> new Object[]{
                rs.getString("sku"), rs.getString("product_sku"), rs.getString("product_name"),
                rs.getString("variant_name"), rs.getBigDecimal("price")});

        Map<String, PricedComponent> result = new HashMap<>();
        for (Object[] row : rows) {
            String variantSku = (String) row[0];
            if (wanted.contains(variantSku)) {
                result.putIfAbsent(variantSku, new PricedComponent(variantSku, (String) row[2], (String) row[3], (BigDecimal) row[4]));
            }
        }
        for (Object[] row : rows) {
            String productSku = (String) row[1];
            if (wanted.contains(productSku)) {
                result.putIfAbsent(productSku, new PricedComponent((String) row[0], (String) row[2], (String) row[3], (BigDecimal) row[4]));
            }
        }
        return result;
    }
}
