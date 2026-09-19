package com.aquarium.inventory.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.inventory.dto.MortalityLogRequest;
import com.aquarium.inventory.dto.MortalityLogResponse;
import com.aquarium.inventory.service.MortalityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/mortality")
@RequiredArgsConstructor
@Tag(name = "Mortality Tracking API", description = "Ghi nhận tỷ lệ hao hụt sinh vật sống và tự động giảm trừ tồn kho (S24)")
public class MortalityController {

    private final MortalityService mortalityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Báo cáo hao hụt cá chết / cây dập nát, tự động trừ kho và ghi log xuất hủy (S24)")
    public ApiResponse<MortalityLogResponse> logMortality(@Valid @RequestBody MortalityLogRequest request) {
        MortalityLogResponse response = mortalityService.logMortality(request);
        return ApiResponse.success("Đã ghi nhận hao hụt và tự động giảm trừ tồn kho", response);
    }

    @GetMapping("/items/{inventoryItemId}")
    @Operation(summary = "Tra cứu lịch sử hao hụt của một mặt hàng sinh vật trong kho")
    public ApiResponse<List<MortalityLogResponse>> getLogsByItem(@PathVariable UUID inventoryItemId) {
        List<MortalityLogResponse> response = mortalityService.getMortalityLogsByItem(inventoryItemId);
        return ApiResponse.success(response);
    }
}
