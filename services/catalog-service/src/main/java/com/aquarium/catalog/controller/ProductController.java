package com.aquarium.catalog.controller;

import com.aquarium.catalog.repository.ProductSpecifications;
import com.aquarium.catalog.dto.ProductDetailResponse;
import com.aquarium.catalog.dto.ProductSummaryResponse;
import com.aquarium.catalog.service.ProductService;
import com.aquarium.common.dto.ApiResponse;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog (U07-U10)", description = "APIs tìm kiếm, lọc và xem thông số kỹ thuật sản phẩm")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;
    /** Chỉ cho sắp xếp theo các trường này (trước đây nhận tên trường tùy ý → lỗi 500). */
    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "createdAt", "createdAt",
            "price", "basePrice",
            "basePrice", "basePrice",
            "name", "name",
            "totalSales", "totalSales",
            "rating", "rating");

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "U07, U09: Danh sách sản phẩm có lọc theo danh mục, combo, 3D và phân trang")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer excludeCategoryId,
            @RequestParam(required = false) Boolean isCombo,
            @RequestParam(required = false) Boolean is3dCustomizable,
            @RequestParam(required = false) Boolean isLivestock,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        String sortField = SORTABLE_FIELDS.get(sortBy);
        if (sortField == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Không hỗ trợ sắp xếp theo '" + sanitize(sortBy) + "'");
        }
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), clampSize(size), sort.and(Sort.by("id")));
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Từ khóa tối đa " + MAX_KEYWORD_LENGTH + " ký tự");
        }
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        ProductSpecifications.Filter filter = new ProductSpecifications.Filter(
                categoryId, excludeCategoryId, isCombo, is3dCustomizable, isLivestock, supplierId, normalizedKeyword);
        return ResponseEntity.ok(ApiResponse.success(productService.getProducts(filter, pageRequest)));
    }

    @GetMapping("/search")
    @Operation(summary = "U08: Tìm kiếm sản phẩm theo từ khóa (tên, mã SKU hoặc mô tả)")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        if (keyword.isBlank() || keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Từ khóa phải từ 1 đến " + MAX_KEYWORD_LENGTH + " ký tự");
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm sản phẩm thành công", productService.searchProducts(keyword, pageRequest)));
    }

    @GetMapping("/3d-combos")
    @Operation(summary = "Lấy tất cả các bộ combo bể thủy sinh nguyên set hỗ trợ phối cảnh 3D")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> get3dCombos() {
        return ResponseEntity.ok(ApiResponse.success(productService.get3dCombos()));
    }

    @GetMapping("/{idOrSlug}")
    @Operation(summary = "U10: Xem chi tiết sản phẩm, danh sách biến thể SKU và bộ sưu tập ảnh")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable String idOrSlug) {
        if (idOrSlug.length() > 280) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        ProductDetailResponse response;
        try {
            response = productService.getProductById(UUID.fromString(idOrSlug));
        } catch (IllegalArgumentException e) {
            response = productService.getProductBySlug(idOrSlug);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static String sanitize(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }
}
