package com.aquarium.inventory.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.inventory.dto.*;
import com.aquarium.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @Operation(summary = "Tra cứu tổng tồn kho và chi tiết từng showroom theo biến thể sản phẩm (S17)")
    public ApiResponse<VariantStockSummaryResponse> getStockByVariant(@PathVariable UUID variantId) {
        VariantStockSummaryResponse response = inventoryService.getStockByVariant(variantId);
        return ApiResponse.success(response);
    }

    @GetMapping("/warehouses/{warehouseId}")
    @Operation(summary = "Xem toàn bộ danh mục mặt hàng lưu trong một kho / showroom (S18)")
    public ApiResponse<List<InventoryItemResponse>> getStockByWarehouse(@PathVariable UUID warehouseId) {
        List<InventoryItemResponse> response = inventoryService.getStockByWarehouse(warehouseId);
        return ApiResponse.success(response);
    }

    @PostMapping("/stock-in")
    @Operation(summary = "Nhập kho hàng mới / Tăng lượng hàng lưu trữ (S19)")
    public ApiResponse<InventoryItemResponse> stockIn(@Valid @RequestBody StockInRequest request) {
        InventoryItemResponse response = inventoryService.stockIn(request);
        return ApiResponse.success("Nhập kho thành công", response);
    }

    @PostMapping("/reserve")
    @Operation(summary = "Khóa tạm thời số lượng tồn kho khi khách đặt mua hàng (S20)")
    public ApiResponse<InventoryItemResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        InventoryItemResponse response = inventoryService.reserveStock(request);
        return ApiResponse.success("Khóa giữ tồn kho thành công", response);
    }

    @PostMapping("/release")
    @Operation(summary = "Nhả lại số lượng tồn kho bị giữ khi khách hủy đơn hoặc hết hạn thanh toán (S21)")
    public ApiResponse<InventoryItemResponse> releaseStock(@Valid @RequestBody ReleaseStockRequest request) {
        InventoryItemResponse response = inventoryService.releaseStock(request);
        return ApiResponse.success("Hủy giữ tồn kho thành công", response);
    }

    @PostMapping("/stock-out")
    @Operation(summary = "Xuất kho giao cho đơn vị vận chuyển (S22)")
    public ApiResponse<InventoryItemResponse> stockOut(@Valid @RequestBody StockOutRequest request) {
        InventoryItemResponse response = inventoryService.stockOut(request);
        return ApiResponse.success("Xuất kho thành công", response);
    }

    @GetMapping("/alerts/low-stock")
    @Operation(summary = "Báo động danh sách mặt hàng chạm ngưỡng tồn kho tối thiểu")
    public ApiResponse<List<InventoryItemResponse>> getLowStockAlerts() {
        List<InventoryItemResponse> response = inventoryService.getLowStockAlerts();
        return ApiResponse.success(response);
    }
}
