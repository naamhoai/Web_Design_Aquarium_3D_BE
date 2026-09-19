package com.aquarium.aquarium3d.controller;

import com.aquarium.aquarium3d.dto.SaveDesignRequest;
import com.aquarium.aquarium3d.dto.UserDesignResponse;
import com.aquarium.aquarium3d.service.UserDesignService;
import com.aquarium.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/3d/designs")
@RequiredArgsConstructor
@Tag(name = "User 3D Designs (D20-D25)", description = "APIs lưu trữ, tải và chia sẻ bản vẽ phối cảnh bể cá 3D")
public class DesignController {

    private final UserDesignService userDesignService;

    @PostMapping
    @Operation(summary = "D20: Lưu bản vẽ phối cảnh 3D của người dùng (tọa độ vật thể và snapshot BOM)")
    public ResponseEntity<ApiResponse<UserDesignResponse>> saveDesign(@Valid @RequestBody SaveDesignRequest request) {
        UserDesignResponse response = userDesignService.saveDesign(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lưu bản thiết kế 3D thành công", response));
    }

    @GetMapping("/share/{slug}")
    @Operation(summary = "D24: Mở bản vẽ 3D từ link chia sẻ công khai (share slug)")
    public ResponseEntity<ApiResponse<UserDesignResponse>> getDesignBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getDesignByShareSlug(slug)));
    }

    @GetMapping("/public")
    @Operation(summary = "Xem danh sách các tác phẩm bể cá 3D xuất sắc do cộng đồng chia sẻ")
    public ResponseEntity<ApiResponse<List<UserDesignResponse>>> getPublicDesigns() {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getPublicDesigns()));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy tất cả các bản vẽ 3D do người dùng đã lưu")
    public ResponseEntity<ApiResponse<List<UserDesignResponse>>> getUserDesigns(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userDesignService.getUserDesigns(userId)));
    }
}
