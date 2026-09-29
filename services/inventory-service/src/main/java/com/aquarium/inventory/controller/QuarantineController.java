package com.aquarium.inventory.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.inventory.dto.QuarantineBatchRequest;
import com.aquarium.inventory.dto.QuarantineBatchResponse;
import com.aquarium.inventory.dto.UpdateQuarantineStatusRequest;
import com.aquarium.inventory.entity.QuarantineStatus;
import com.aquarium.inventory.service.QuarantineService;
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
@RequestMapping("/api/v1/inventory/quarantine")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Livestock Quarantine API", description = "Quy trình cách ly, kiểm dịch đàn sinh vật sống (S15)")
public class QuarantineController {

    private final QuarantineService quarantineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tiếp nhận lô cá mới vào khu cách ly (chưa tính vào tồn kho bán được)")
    public ApiResponse<QuarantineBatchResponse> registerBatch(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @Valid @RequestBody QuarantineBatchRequest request) {
        return ApiResponse.success("Đã tiếp nhận lô cá vào chế độ kiểm dịch", quarantineService.registerQuarantineBatch(user, request));
    }

    @GetMapping
    @Operation(summary = "Danh sách lô kiểm dịch (nhà cung cấp chỉ thấy kho của mình)")
    public ApiResponse<List<QuarantineBatchResponse>> getAllBatches(@AuthenticationPrincipal AuthenticatedUser user,
                                                                    @RequestParam(required = false) QuarantineStatus status) {
        return ApiResponse.success(quarantineService.getAllQuarantineBatches(user, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một lô kiểm dịch")
    public ApiResponse<QuarantineBatchResponse> getBatchById(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @PathVariable UUID id) {
        return ApiResponse.success(quarantineService.getQuarantineById(user, id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Kết luận kiểm dịch: PASSED (cộng cá khỏe vào tồn kho) hoặc FAILED_INFECTED")
    public ApiResponse<QuarantineBatchResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @PathVariable UUID id,
                                                             @Valid @RequestBody UpdateQuarantineStatusRequest request) {
        return ApiResponse.success("Cập nhật trạng thái kiểm dịch thành công", quarantineService.updateQuarantineStatus(user, id, request));
    }
}
