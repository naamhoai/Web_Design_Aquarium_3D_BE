package com.aquarium.common.security;

import java.util.UUID;

/**
 * Người dùng đã xác thực, được dựng từ JWT access token (không truy vấn DB).
 * Mọi service lấy danh tính từ đây thay vì tin userId do client gửi lên.
 */
public record AuthenticatedUser(UUID id, String email, String role) {

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_SUPPLIER = "SUPPLIER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TECHNICIAN = "TECHNICIAN";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean hasRole(String expectedRole) {
        return expectedRole != null && expectedRole.equals(role);
    }
}
