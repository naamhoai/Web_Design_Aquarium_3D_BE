package com.aquarium.supplier.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.supplier.dto.RegisterSupplierRequest;
import com.aquarium.supplier.dto.SupplierResponse;
import com.aquarium.supplier.dto.UpdateSupplierRequest;
import com.aquarium.supplier.entity.SupplierStatus;
import com.aquarium.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers API", description = "Quản lý đối tác nhà cung cấp, hồ sơ showroom và kiểm duyệt")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký trở thành nhà cung cấp (S01)")
    public ApiResponse<SupplierResponse> registerSupplier(@Valid @RequestBody RegisterSupplierRequest request) {
        SupplierResponse response = supplierService.registerSupplier(request);
        return ApiResponse.success("Đăng ký đối tác thành công, đang chờ ban quản trị phê duyệt", response);
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "Xem thông tin chi tiết nhà cung cấp theo ID hoặc Slug (S02)")
    public ApiResponse<SupplierResponse> getSupplier(@PathVariable String idOrSlug) {
        SupplierResponse response = supplierService.getSupplierByIdOrSlug(idOrSlug);
        return ApiResponse.success(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy hồ sơ nhà cung cấp theo User ID")
    public ApiResponse<SupplierResponse> getSupplierByUser(@PathVariable UUID userId) {
        SupplierResponse response = supplierService.getSupplierByUserId(userId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin gian hàng nhà cung cấp (S04)")
    public ApiResponse<SupplierResponse> updateSupplier(
            @PathVariable UUID id,
            @RequestBody UpdateSupplierRequest request) {
        SupplierResponse response = supplierService.updateSupplier(id, request);
        return ApiResponse.success("Cập nhật thông tin đối tác thành công", response);
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả nhà cung cấp (Admin / Lọc theo trạng thái)")
    public ApiResponse<List<SupplierResponse>> getAllSuppliers(
            @RequestParam(required = false) SupplierStatus status) {
        List<SupplierResponse> response = supplierService.getAllSuppliers(status);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Phê duyệt hoặc thay đổi trạng thái đối tác (A06-A07)")
    public ApiResponse<SupplierResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam SupplierStatus status,
            @RequestParam(required = false) BigDecimal commissionRate) {
        SupplierResponse response = supplierService.updateSupplierStatus(id, status, commissionRate);
        return ApiResponse.success("Cập nhật trạng thái đối tác thành công", response);
    }
}
