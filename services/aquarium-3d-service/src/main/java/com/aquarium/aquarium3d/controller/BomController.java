package com.aquarium.aquarium3d.controller;

import com.aquarium.aquarium3d.dto.ExplodedBomResponse;
import com.aquarium.aquarium3d.service.BomService;
import com.aquarium.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/3d/boms")
@RequiredArgsConstructor
@Tag(name = "3D Modular BOM (D01-D06)", description = "APIs bóc tách cấu trúc 6 tầng bể cá và tráo đổi linh kiện")
public class BomController {

    private final BomService bomService;

    @GetMapping("/{productIdOrSku}/exploded-view")
    @Operation(summary = "D01-D06: Lấy cấu trúc bóc tách 6 tầng linh kiện của Combo Bể Thủy Sinh 3D")
    public ResponseEntity<ApiResponse<ExplodedBomResponse>> getExplodedBom(@PathVariable String productIdOrSku) {
        ExplodedBomResponse response;
        try {
            UUID id = UUID.fromString(productIdOrSku);
            response = bomService.getExplodedBom(id);
        } catch (IllegalArgumentException e) {
            response = bomService.getExplodedBomBySku(productIdOrSku);
        }

        return ResponseEntity.ok(ApiResponse.success("Bóc tách 6 tầng linh kiện bể cá thành công", response));
    }
}
