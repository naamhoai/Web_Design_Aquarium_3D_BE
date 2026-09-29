package com.aquarium.supplier.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.supplier.dto.RegisterSupplierRequest;
import com.aquarium.supplier.dto.SupplierResponse;
import com.aquarium.supplier.dto.UpdateSupplierRequest;
import com.aquarium.supplier.entity.SupplierStatus;
import com.aquarium.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers API", description = "Đối tác nhà cung cấp: đăng ký, hồ sơ gian hàng và kiểm duyệt")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký tài khoản đang đăng nhập làm nhà cung cấp (chờ admin duyệt)",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<SupplierResponse> registerSupplier(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @Valid @RequestBody RegisterSupplierRequest request) {
        return ApiResponse.success("Đăng ký đối tác thành công, đang chờ ban quản trị phê duyệt",
                supplierService.registerSupplier(user, request));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // "/suppliers/*" là public cho GET nên phải chặn riêng ở đây
    @Operation(summary = "Hồ sơ nhà cung cấp của tôi", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<SupplierResponse> getMySupplier(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(supplierService.getMySupplier(user));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Trang gian hàng công khai theo ID hoặc slug")
    public ApiResponse<SupplierResponse> getSupplier(@PathVariable String idOrSlug,
                                                     @AuthenticationPrincipal AuthenticatedUser viewer) {
        return ApiResponse.success(supplierService.getSupplierByIdOrSlug(idOrSlug, viewer));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hồ sơ gian hàng (chủ shop hoặc admin)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<SupplierResponse> updateSupplier(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody UpdateSupplierRequest request) {
        return ApiResponse.success("Cập nhật thông tin đối tác thành công", supplierService.updateSupplier(user, id, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: danh sách nhà cung cấp (lọc theo trạng thái)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<List<SupplierResponse>> getAllSuppliers(@RequestParam(required = false) SupplierStatus status) {
        return ApiResponse.success(supplierService.getAllSuppliers(status));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: phê duyệt / tạm khóa gian hàng và đặt hoa hồng (A04-A07)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<SupplierResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser admin,
                                                      @PathVariable UUID id,
                                                      @RequestParam SupplierStatus status,
                                                      @RequestParam(required = false) BigDecimal commissionRate) {
        return ApiResponse.success("Cập nhật trạng thái đối tác thành công",
                supplierService.updateSupplierStatus(admin, id, status, commissionRate));
    }
}
