package com.aquarium.identity.service;

import com.aquarium.identity.dto.AuthResponse;
import com.aquarium.identity.dto.LoginRequest;
import com.aquarium.identity.dto.RegisterRequest;
import com.aquarium.identity.dto.ChangePasswordRequest;
import com.aquarium.identity.dto.UpdateProfileRequest;
import com.aquarium.identity.dto.UserProfileResponse;

import java.util.UUID;

public interface AuthService {

    /** Kết quả đăng nhập: body trả cho client + refresh token thô để controller đặt vào cookie. */
    record AuthResult(AuthResponse response, String refreshToken) {
    }

    AuthResult register(RegisterRequest request, String clientIp);

    AuthResult login(LoginRequest request, String clientIp);

    AuthResult refresh(String refreshToken);

    void logout(String refreshToken);

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /** Đổi mật khẩu: thu hồi mọi refresh token cũ (đăng xuất thiết bị khác) và cấp phiên mới cho thiết bị hiện tại. */
    AuthResult changePassword(UUID userId, ChangePasswordRequest request, String clientIp);
}
