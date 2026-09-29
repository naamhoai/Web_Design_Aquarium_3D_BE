package com.aquarium.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cookie chứa refresh token: HttpOnly (JavaScript không đọc được → chống đánh cắp qua XSS),
 * SameSite=Strict (chống CSRF), chỉ gửi kèm các request tới /api/v1/auth.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.refresh-cookie")
public class AuthCookieProperties {

    private String name = "aq_refresh";

    private String path = "/api/v1/auth";

    /** Luôn bật khi chạy HTTPS. Trình duyệt Chrome/Edge/Firefox vẫn chấp nhận cookie Secure trên http://localhost. */
    private boolean secure = true;

    private String sameSite = "Strict";
}
