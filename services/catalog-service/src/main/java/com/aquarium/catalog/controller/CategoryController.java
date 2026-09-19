package com.aquarium.catalog.controller;

import com.aquarium.catalog.dto.CategoryResponse;
import com.aquarium.catalog.service.CategoryService;
import com.aquarium.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Product Categories (U07)", description = "APIs duyệt và khám phá cây danh mục sản phẩm bể cá")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Lấy tất cả danh mục sản phẩm đang hoạt động")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/tree")
    @Operation(summary = "U07: Lấy cây danh mục sản phẩm đa tầng (phân cấp cha-con)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Xem chi tiết một danh mục theo đường dẫn tĩnh (slug)")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryBySlug(slug)));
    }
}
