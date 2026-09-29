package com.aquarium.order.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.*;

/** Tra cứu quan hệ người dùng ↔ nhà cung cấp để kiểm tra quyền sở hữu (chống IDOR). */
@Component
@RequiredArgsConstructor
public class SupplierAccess {

    private final JdbcTemplate jdbcTemplate;

    /** Id nhà cung cấp ĐANG HOẠT ĐỘNG thuộc về user (rỗng nếu user không phải chủ shop). */
    public Optional<UUID> findActiveSupplierIdByUser(UUID userId) {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM suppliers WHERE user_id = ? AND status = 'ACTIVE' AND deleted_at IS NULL",
                (rs, i) -> rs.getObject(1, UUID.class), userId);
        return ids.stream().findFirst();
    }

    public Map<UUID, String> storeNames(Collection<UUID> supplierIds) {
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement("SELECT id, store_name FROM suppliers WHERE id = ANY(?)");
            ps.setArray(1, connection.createArrayOf("uuid", new LinkedHashSet<>(supplierIds).toArray()));
            return ps;
        }, rs -> {
            names.put(rs.getObject("id", UUID.class), rs.getString("store_name"));
        });
        return names;
    }
}
