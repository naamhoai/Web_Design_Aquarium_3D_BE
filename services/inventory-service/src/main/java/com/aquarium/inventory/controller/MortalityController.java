package com.aquarium.inventory.controller;

import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.inventory.dto.MortalityLogRequest;
import com.aquarium.inventory.dto.MortalityLogResponse;
import com.aquarium.inventory.service.MortalityService;
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
@RequestMapping("/api/v1/inventory/mortality")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Mortality Tracking API", description = "Ghi nhận hao hụt sinh vật sống và tự động giảm trừ tồn kho (S16)")
public class MortalityController {

    private final MortalityService mortalityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Báo cáo hao hụt, tự động trừ kho và ghi log xuất hủy")
    public ApiResponse<MortalityLogResponse> logMortality(@AuthenticationPrincipal AuthenticatedUser user,
                                                          @Valid @RequestBody MortalityLogRequest request) {
        return ApiResponse.success("Đã ghi nhận hao hụt và tự động giảm trừ tồn kho", mortalityService.logMortality(user, request));
    }

    @GetMapping("/items/{inventoryItemId}")
    @Operation(summary = "Lịch sử hao hụt của một mặt hàng trong kho")
    public ApiResponse<List<MortalityLogResponse>> getLogsByItem(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @PathVariable UUID inventoryItemId) {
        return ApiResponse.success(mortalityService.getMortalityLogsByItem(user, inventoryItemId));
    }
}
