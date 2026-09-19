package com.aquarium.aquarium3d.controller;

import com.aquarium.aquarium3d.dto.CompatibilityCheckRequest;
import com.aquarium.aquarium3d.dto.CompatibilityCheckResponse;
import com.aquarium.aquarium3d.entity.BiologicalRule;
import com.aquarium.aquarium3d.service.BiologyEngineService;
import com.aquarium.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/3d/biology")
@RequiredArgsConstructor
@Tag(name = "Biology Rules Engine (D07-D10)", description = "APIs kiểm tra tương thích sinh thái, thể tích bể và độ an toàn bio-load")
public class BiologyEngineController {

    private final BiologyEngineService biologyEngineService;

    @GetMapping("/rules")
    @Operation(summary = "Lấy toàn bộ quy chuẩn sinh học của các loài cá, tép cảnh (pH, nhiệt độ, bio-load)")
    public ResponseEntity<ApiResponse<List<BiologicalRule>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success(biologyEngineService.getAllRules()));
    }

    @PostMapping("/check-compatibility")
    @Operation(summary = "D07-D10: Kiểm tra tương thích sinh học giữa các loài sinh vật được chọn vào bể 3D")
    public ResponseEntity<ApiResponse<CompatibilityCheckResponse>> checkCompatibility(
            @Valid @RequestBody CompatibilityCheckRequest request
    ) {
        CompatibilityCheckResponse response = biologyEngineService.checkCompatibility(request);
        return ResponseEntity.ok(ApiResponse.success("Kiểm tra tương thích sinh học hoàn tất", response));
    }
}
