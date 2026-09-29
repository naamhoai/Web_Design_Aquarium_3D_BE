package com.aquarium.identity.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AquariumSecurityProperties;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.common.security.ClientIpResolver;
import com.aquarium.identity.config.AuthCookies;
import com.aquarium.identity.dto.AuthResponse;
import com.aquarium.identity.dto.LoginRequest;
import com.aquarium.identity.dto.RegisterRequest;
import com.aquarium.identity.dto.UserProfileResponse;
import com.aquarium.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity (U01-U05)", description = "APIs quản lý đăng ký, đăng nhập và tài khoản người dùng")
public class AuthController {

    /**
     * Header tùy chỉnh bắt buộc cho các endpoint dùng cookie (refresh/logout).
     * Form HTML của trang web khác không thể gửi header tùy chỉnh, và request cross-origin có header này
     * sẽ bị preflight CORS chặn → chống CSRF.
     */
    static final String CLIENT_HEADER = "X-Aquarium-Client";

    private final AuthService authService;
    private final AuthCookies authCookies;
    private final AquariumSecurityProperties securityProperties;

    @PostMapping("/register")
    @Operation(summary = "U01: Đăng ký tài khoản khách hàng mới")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.register(request, clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookies.refresh(result.refreshToken()).toString())
                .body(ApiResponse.success("Đăng ký tài khoản thành công", result.response()));
    }

    @PostMapping("/login")
    @Operation(summary = "U02: Đăng nhập (access token trong body, refresh token trong cookie HttpOnly)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthService.AuthResult result = authService.login(request, clientIp(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.refresh(result.refreshToken()).toString())
                .body(ApiResponse.success("Đăng nhập thành công", result.response()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token bằng refresh token (cookie). Token cũ bị thu hồi.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest httpRequest,
                                                             @RequestHeader(value = CLIENT_HEADER, required = false) String client) {
        requireClientHeader(client);
        String token = authCookies.read(httpRequest);
        try {
            AuthService.AuthResult result = authService.refresh(token);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, authCookies.refresh(result.refreshToken()).toString())
                    .body(ApiResponse.success("Làm mới phiên đăng nhập thành công", result.response()));
        } catch (AppException e) {
            return ResponseEntity.status(e.getErrorCode().getStatusCode())
                    .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                    .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "U03: Đăng xuất — thu hồi refresh token và xóa cookie")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest,
                                                    @RequestHeader(value = CLIENT_HEADER, required = false) String client) {
        requireClientHeader(client);
        authService.logout(authCookies.read(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.clear().toString())
                .body(ApiResponse.success("Đã đăng xuất", null));
    }

    @GetMapping("/me")
    @Operation(summary = "U05: Xem thông tin cá nhân hiện tại", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        UserProfileResponse profile = authService.getProfile(currentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", profile));
    }

    private String clientIp(HttpServletRequest request) {
        return ClientIpResolver.resolve(request, securityProperties.getTrustedProxies());
    }

    private static void requireClientHeader(String client) {
        if (client == null || client.isBlank() || client.length() > 32) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Thiếu header xác thực client");
        }
    }
}
