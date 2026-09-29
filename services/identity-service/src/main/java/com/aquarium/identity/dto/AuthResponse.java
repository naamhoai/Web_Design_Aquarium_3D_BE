package com.aquarium.identity.dto;

import lombok.*;

/**
 * Chỉ trả access token trong body. Refresh token KHÔNG nằm trong body mà được đặt
 * trong cookie HttpOnly để JavaScript (và mã độc XSS) không thể đọc.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    /** Số giây access token còn hiệu lực. */
    private long expiresIn;
    private UserProfileResponse user;
}
