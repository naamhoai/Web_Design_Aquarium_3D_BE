package com.aquarium.identity.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.identity.dto.AuthResponse;
import com.aquarium.identity.dto.LoginRequest;
import com.aquarium.identity.dto.RegisterRequest;
import com.aquarium.identity.dto.UserProfileResponse;
import com.aquarium.identity.entity.User;
import com.aquarium.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & Identity (U01-U05)", description = "APIs quản lý đăng ký, đăng nhập và tài khoản người dùng")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "U01: Đăng ký tài khoản khách hàng mới")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "U02: Đăng nhập hệ thống (Lấy JWT Access & Refresh Token)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @GetMapping("/me")
    @Operation(summary = "U05: Xem thông tin cá nhân hiện tại", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserProfileResponse profile = authService.getProfile(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", profile));
    }
}
