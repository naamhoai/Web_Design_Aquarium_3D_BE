package com.aquarium.identity.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AquariumSecurityProperties;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.common.security.ClientIpResolver;
import com.aquarium.identity.config.AuthCookies;
import com.aquarium.identity.dto.AddressRequest;
import com.aquarium.identity.dto.AddressResponse;
import com.aquarium.identity.dto.AuthResponse;
import com.aquarium.identity.dto.ChangePasswordRequest;
import com.aquarium.identity.dto.UpdateProfileRequest;
import com.aquarium.identity.dto.UserProfileResponse;
import com.aquarium.identity.service.AddressService;
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

import java.util.List;
import java.util.UUID;

/** Tài khoản của chính người đang đăng nhập — không nhận userId từ client. */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Account", description = "Hồ sơ, đổi mật khẩu và sổ địa chỉ của người dùng đang đăng nhập")
public class AccountController {

    private final AuthService authService;
    private final AddressService addressService;
    private final AuthCookies authCookies;
    private final AquariumSecurityProperties securityProperties;

    @GetMapping("/profile")
    @Operation(summary = "Hồ sơ của tôi")
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(authService.getProfile(user.id()));
    }

    @PatchMapping("/profile")
    @Operation(summary = "Cập nhật họ tên / số điện thoại / ảnh đại diện")
    public ApiResponse<UserProfileResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Đã cập nhật hồ sơ", authService.updateProfile(user.id(), request));
    }

    @PutMapping("/password")
    @Operation(summary = "Đổi mật khẩu — đăng xuất mọi thiết bị khác, giữ phiên hiện tại")
    public ResponseEntity<ApiResponse<AuthResponse>> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @Valid @RequestBody ChangePasswordRequest request,
                                                                    HttpServletRequest httpRequest) {
        String clientIp = ClientIpResolver.resolve(httpRequest, securityProperties.getTrustedProxies());
        AuthService.AuthResult result = authService.changePassword(user.id(), request, clientIp);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookies.refresh(result.refreshToken()).toString())
                .body(ApiResponse.success("Đổi mật khẩu thành công, các thiết bị khác đã được đăng xuất", result.response()));
    }

    @GetMapping("/addresses")
    @Operation(summary = "Sổ địa chỉ của tôi")
    public ApiResponse<List<AddressResponse>> listAddresses(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(addressService.list(user.id()));
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm địa chỉ nhận hàng")
    public ApiResponse<AddressResponse> createAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("Đã thêm địa chỉ", addressService.create(user.id(), request));
    }

    @PutMapping("/addresses/{id}")
    @Operation(summary = "Sửa địa chỉ nhận hàng")
    public ApiResponse<AddressResponse> updateAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success("Đã cập nhật địa chỉ", addressService.update(user.id(), id, request));
    }

    @DeleteMapping("/addresses/{id}")
    @Operation(summary = "Xóa địa chỉ nhận hàng")
    public ApiResponse<Void> deleteAddress(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        addressService.delete(user.id(), id);
        return ApiResponse.success("Đã xóa địa chỉ", null);
    }

    @PatchMapping("/addresses/{id}/default")
    @Operation(summary = "Đặt làm địa chỉ mặc định")
    public ApiResponse<AddressResponse> setDefaultAddress(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PathVariable UUID id) {
        return ApiResponse.success("Đã đặt làm địa chỉ mặc định", addressService.setDefault(user.id(), id));
    }
}
