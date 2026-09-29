package com.aquarium.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cấu hình JWT dùng chung. {@code jwt.secret} BẮT BUỘC lấy từ biến môi trường JWT_SECRET,
 * không có giá trị mặc định trong mã nguồn.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** Khóa ký HMAC-SHA256, tối thiểu 32 ký tự. */
    private String secret;

    private String issuer = "aquarium-identity";

    private String audience = "aquarium-api";

    /** Access token ngắn hạn để giảm thiệt hại nếu bị lộ. */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /** Refresh token (opaque, lưu băm trong DB, xoay vòng mỗi lần dùng). */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** Dung sai lệch đồng hồ giữa các máy chủ. */
    private Duration clockSkew = Duration.ofSeconds(30);
}
