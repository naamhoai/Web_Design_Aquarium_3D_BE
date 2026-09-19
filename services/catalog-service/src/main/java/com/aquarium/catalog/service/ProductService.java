package com.aquarium.catalog.service;

import com.aquarium.catalog.dto.ProductDetailResponse;
import com.aquarium.catalog.dto.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    Page<ProductSummaryResponse> getProducts(Integer categoryId, Boolean isCombo, Boolean is3dCustomizable, Pageable pageable);
    Page<ProductSummaryResponse> searchProducts(String keyword, Pageable pageable);
    List<ProductSummaryResponse> get3dCombos();
    ProductDetailResponse getProductBySlug(String slug);
    ProductDetailResponse getProductById(UUID id);
}
