package com.aquarium.catalog.controller;

import com.aquarium.catalog.dto.ProductDetailResponse;
import com.aquarium.catalog.dto.ProductSummaryResponse;
import com.aquarium.catalog.service.ProductService;
import com.aquarium.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog (U07-U10)", description = "APIs tìm kiếm, lọc và xem thông số kỹ thuật sản phẩm")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "U07, U09: Danh sách sản phẩm có lọc theo danh mục, giá tiền và phân trang")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean isCombo,
            @RequestParam(required = false) Boolean is3dCustomizable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<ProductSummaryResponse> result = productService.getProducts(categoryId, isCombo, is3dCustomizable, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/search")
    @Operation(summary = "U08: Tìm kiếm sản phẩm theo từ khóa (tên, mã SKU hoặc mô tả)")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ProductSummaryResponse> result = productService.searchProducts(keyword, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm sản phẩm thành công", result));
    }

    @GetMapping("/3d-combos")
    @Operation(summary = "Lấy tất cả các bộ combo bể thủy sinh nguyên set hỗ trợ phối cảnh 3D")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> get3dCombos() {
        return ResponseEntity.ok(ApiResponse.success(productService.get3dCombos()));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "U10: Xem chi tiết sản phẩm, danh sách biến thể SKU và bộ sưu tập ảnh")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable String idOrSlug) {
        ProductDetailResponse response;
        try {
            UUID id = UUID.fromString(idOrSlug);
            response = productService.getProductById(id);
        } catch (IllegalArgumentException e) {
            response = productService.getProductBySlug(idOrSlug);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
