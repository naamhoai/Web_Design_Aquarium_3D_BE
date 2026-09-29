package com.aquarium.aquarium3d.controller;

import com.aquarium.aquarium3d.dto.SaveDesignRequest;
import com.aquarium.aquarium3d.dto.UserDesignResponse;
import com.aquarium.aquarium3d.service.UserDesignService;
import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/3d/designs")
@RequiredArgsConstructor
@Tag(name = "User 3D Designs (D25)", description = "APIs lưu trữ, tải và chia sẻ bản vẽ phối cảnh bể cá 3D")
public class DesignController {

    private final UserDesignService userDesignService;

    @PostMapping
    @Operation(summary = "Lưu bản vẽ 3D của người dùng đang đăng nhập (giá do server tính)",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<UserDesignResponse>> saveDesign(@AuthenticationPrincipal AuthenticatedUser user,
                                                                      @Valid @RequestBody SaveDesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu bản thiết kế 3D thành công", userDesignService.saveDesign(user.id(), request)));
    }

    @GetMapping("/share/{slug}")
    @Operation(summary = "Mở bản vẽ 3D từ link chia sẻ (thiết kế riêng tư chỉ chủ sở hữu xem được)")
    public ResponseEntity<ApiResponse<UserDesignResponse>> getDesignBySlug(@PathVariable String slug,
                                                                           @AuthenticationPrincipal AuthenticatedUser viewer) {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getDesignByShareSlug(slug, viewer)));
    }

    @GetMapping("/public")
    @Operation(summary = "50 tác phẩm bể cá 3D công khai nổi bật của cộng đồng")
    public ResponseEntity<ApiResponse<List<UserDesignResponse>>> getPublicDesigns() {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getPublicDesigns()));
    }

    @GetMapping("/me")
    @Operation(summary = "Các bản vẽ 3D của tôi (kể cả riêng tư)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<List<UserDesignResponse>>> getMyDesigns(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getMyDesigns(user.id())));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Các bản vẽ 3D CÔNG KHAI của một người dùng")
    public ResponseEntity<ApiResponse<List<UserDesignResponse>>> getUserDesigns(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getPublicDesignsOfUser(userId)));
    }
}
