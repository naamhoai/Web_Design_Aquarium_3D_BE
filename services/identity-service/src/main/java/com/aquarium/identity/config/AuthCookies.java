package com.aquarium.identity.config;

import com.aquarium.common.security.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/** Tạo / đọc cookie refresh token (HttpOnly) — dùng chung cho các controller của identity-service. */
@Component
@RequiredArgsConstructor
public class AuthCookies {

    private final AuthCookieProperties properties;
    private final JwtProperties jwtProperties;

    public ResponseCookie refresh(String value) {
        return base(value).maxAge(jwtProperties.getRefreshTokenTtl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public String read(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.getName());
        return cookie != null ? cookie.getValue() : null;
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.getName(), value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(properties.getPath());
    }
}
