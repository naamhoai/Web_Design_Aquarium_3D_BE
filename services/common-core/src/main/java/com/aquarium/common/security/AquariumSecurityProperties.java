package com.aquarium.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình bảo mật riêng từng service.
 * <pre>
 * aquarium:
 *   security:
 *     public-endpoints:
 *       - GET /api/v1/products/**     # chỉ GET được truy cập ẩn danh
 *       - /api/v1/auth/login          # mọi method
 *     max-request-bytes: 262144
 * </pre>
 * Mặc định mọi endpoint không liệt kê đều YÊU CẦU đăng nhập (deny-by-default).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "aquarium.security")
public class AquariumSecurityProperties {

    private List<String> publicEndpoints = new ArrayList<>();

    /** Giới hạn kích thước body của request (byte). */
    private long maxRequestBytes = 256 * 1024;

    /** Địa chỉ proxy tin cậy (API Gateway) được phép gửi X-Forwarded-For. */
    private List<String> trustedProxies = new ArrayList<>(List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1"));
}
