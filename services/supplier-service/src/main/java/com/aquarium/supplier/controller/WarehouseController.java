package com.aquarium.supplier.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.supplier.dto.CreateWarehouseRequest;
import com.aquarium.supplier.dto.WarehouseResponse;
import com.aquarium.supplier.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Warehouses API", description = "Mạng lưới showroom và kho hàng của nhà cung cấp")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping("/suppliers/{supplierId}/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Thêm kho / showroom (chủ shop đã được duyệt hoặc admin)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<WarehouseResponse> createWarehouse(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PathVariable UUID supplierId,
                                                          @Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.success("Tạo kho hàng thành công", warehouseService.createWarehouse(user, supplierId, request));
    }

    @GetMapping("/suppliers/{supplierId}/warehouses")
    @Operation(summary = "Danh sách kho / showroom của một nhà cung cấp")
    public ApiResponse<List<WarehouseResponse>> getWarehousesBySupplier(@PathVariable UUID supplierId,
                                                                        @AuthenticationPrincipal AuthenticatedUser viewer) {
        return ApiResponse.success(warehouseService.getWarehousesBySupplier(supplierId, viewer));
    }

    @GetMapping("/warehouses/{id}")
    @Operation(summary = "Chi tiết kho hàng / showroom")
    public ApiResponse<WarehouseResponse> getWarehouseById(@PathVariable UUID id,
                                                           @AuthenticationPrincipal AuthenticatedUser viewer) {
        return ApiResponse.success(warehouseService.getWarehouseById(id, viewer));
    }

    @PatchMapping("/warehouses/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Bật / tắt kho hàng (chủ shop hoặc admin)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<WarehouseResponse> toggleWarehouse(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @PathVariable UUID id,
                                                          @RequestParam boolean active) {
        return ApiResponse.success("Cập nhật trạng thái kho thành công", warehouseService.toggleWarehouseStatus(user, id, active));
    }
}
