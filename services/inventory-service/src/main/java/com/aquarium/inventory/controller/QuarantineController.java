package com.aquarium.inventory.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.inventory.dto.QuarantineBatchRequest;
import com.aquarium.inventory.dto.QuarantineBatchResponse;
import com.aquarium.inventory.dto.UpdateQuarantineStatusRequest;
import com.aquarium.inventory.entity.QuarantineStatus;
import com.aquarium.inventory.service.QuarantineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/quarantine")
@RequiredArgsConstructor
@Tag(name = "Livestock Quarantine API", description = "Quản lý quy trình cách ly, theo dõi kiểm dịch đàn sinh vật sống (S23)")
public class QuarantineController {

    private final QuarantineService quarantineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tiếp nhận lô cá cảnh mới vào trại cách ly kiểm dịch (S23)")
    public ApiResponse<QuarantineBatchResponse> registerBatch(@Valid @RequestBody QuarantineBatchRequest request) {
        QuarantineBatchResponse response = quarantineService.registerQuarantineBatch(request);
        return ApiResponse.success("Đã tiếp nhận lô cá vào chế độ kiểm dịch", response);
    }

    @GetMapping
    @Operation(summary = "Tra cứu danh sách các lô kiểm dịch theo trạng thái")
    public ApiResponse<List<QuarantineBatchResponse>> getAllBatches(
            @RequestParam(required = false) QuarantineStatus status) {
        List<QuarantineBatchResponse> response = quarantineService.getAllQuarantineBatches(status);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một lô kiểm dịch cá cảnh")
    public ApiResponse<QuarantineBatchResponse> getBatchById(@PathVariable UUID id) {
        QuarantineBatchResponse response = quarantineService.getQuarantineById(id);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật kết quả đánh giá kiểm dịch (Đạt PASSED hoặc Nhiễm bệnh FAILED)")
    public ApiResponse<QuarantineBatchResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuarantineStatusRequest request) {
        QuarantineBatchResponse response = quarantineService.updateQuarantineStatus(id, request);
        return ApiResponse.success("Cập nhật trạng thái kiểm dịch thành công", response);
    }
}
