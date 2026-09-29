package com.aquarium.common.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;

/**
 * Xác định IP thật của client.
 * Chỉ tin header X-Forwarded-For khi request đến từ proxy tin cậy (API Gateway chạy cùng máy);
 * khi đó lấy phần tử CUỐI cùng — phần tử do chính gateway thêm vào, client không giả mạo được.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, Collection<String> trustedProxies) {
        String remote = request.getRemoteAddr();
        if (remote != null && trustedProxies.contains(remote)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                String last = parts[parts.length - 1].trim();
                if (!last.isEmpty() && last.length() <= 45) {
                    return last;
                }
            }
        }
        return remote != null ? remote : "unknown";
    }
}
