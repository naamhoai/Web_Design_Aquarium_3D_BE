package com.aquarium.inventory.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.inventory.dto.*;
import com.aquarium.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory API", description = "Quản lý tồn kho đa điểm, kiểm tra khả dụng, giữ hàng và xuất nhập kho")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Công khai: tổng tồn kho và chi tiết từng showroom theo biến thể sản phẩm")
    public ApiResponse<VariantStockSummaryResponse> getStockByVariant(@PathVariable UUID variantId) {
        return ApiResponse.success(inventoryService.getStockByVariant(variantId));
    }

    @GetMapping("/warehouses/{warehouseId}")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Danh mục hàng trong một kho (chủ kho hoặc admin)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<List<InventoryItemResponse>> getStockByWarehouse(@AuthenticationPrincipal AuthenticatedUser user,
                                                                         @PathVariable UUID warehouseId) {
        return ApiResponse.success(inventoryService.getStockByWarehouse(user, warehouseId));
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Nhập kho (chủ kho hoặc admin)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<InventoryItemResponse> stockIn(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody StockInRequest request) {
        return ApiResponse.success("Nhập kho thành công", inventoryService.stockIn(user, request));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: giữ hàng thủ công (luồng đặt hàng tự giữ hàng)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<InventoryItemResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        return ApiResponse.success("Khóa giữ tồn kho thành công", inventoryService.reserveStock(request));
    }

    @PostMapping("/release")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: nhả hàng đang giữ thủ công", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<InventoryItemResponse> releaseStock(@Valid @RequestBody ReleaseStockRequest request) {
        return ApiResponse.success("Hủy giữ tồn kho thành công", inventoryService.releaseStock(request));
    }

    @PostMapping("/stock-out")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Xuất kho (chủ kho hoặc admin)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<InventoryItemResponse> stockOut(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @Valid @RequestBody StockOutRequest request) {
        return ApiResponse.success("Xuất kho thành công", inventoryService.stockOut(user, request));
    }

    @GetMapping("/alerts/low-stock")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    @Operation(summary = "Mặt hàng sắp hết (nhà cung cấp chỉ thấy kho của mình)", security = @SecurityRequirement(name = "BearerAuth"))
    public ApiResponse<List<InventoryItemResponse>> getLowStockAlerts(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(inventoryService.getLowStockAlerts(user));
    }
}
