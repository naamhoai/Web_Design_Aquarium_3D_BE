package com.aquarium.supplier.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.supplier.dto.CreateWarehouseRequest;
import com.aquarium.supplier.dto.WarehouseResponse;
import com.aquarium.supplier.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Warehouses API", description = "Quản lý mạng lưới showroom và kho hàng của nhà cung cấp")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping("/suppliers/{supplierId}/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm mới kho hàng / showroom cho nhà cung cấp (S05)")
    public ApiResponse<WarehouseResponse> createWarehouse(
            @PathVariable UUID supplierId,
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.createWarehouse(supplierId, request);
        return ApiResponse.success("Tạo kho hàng thành công", response);
    }

    @GetMapping("/suppliers/{supplierId}/warehouses")
    @Operation(summary = "Lấy danh sách tất cả kho / showroom của một nhà cung cấp (S06)")
    public ApiResponse<List<WarehouseResponse>> getWarehousesBySupplier(@PathVariable UUID supplierId) {
        List<WarehouseResponse> response = warehouseService.getWarehousesBySupplier(supplierId);
        return ApiResponse.success(response);
    }

    @GetMapping("/warehouses/{id}")
    @Operation(summary = "Xem chi tiết thông tin kho hàng")
    public ApiResponse<WarehouseResponse> getWarehouseById(@PathVariable UUID id) {
        WarehouseResponse response = warehouseService.getWarehouseById(id);
        return ApiResponse.success(response);
    }

    @PatchMapping("/warehouses/{id}/toggle-active")
    @Operation(summary = "Bật / Tắt trạng thái hoạt động của kho hàng")
    public ApiResponse<WarehouseResponse> toggleWarehouse(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        WarehouseResponse response = warehouseService.toggleWarehouseStatus(id, active);
        return ApiResponse.success("Cập nhật trạng thái kho thành công", response);
    }
}
