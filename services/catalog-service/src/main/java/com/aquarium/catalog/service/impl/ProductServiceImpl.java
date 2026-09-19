package com.aquarium.catalog.service.impl;

import com.aquarium.catalog.dto.*;
import com.aquarium.catalog.entity.*;
import com.aquarium.catalog.repository.*;
import com.aquarium.catalog.service.ProductService;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getProducts(Integer categoryId, Boolean isCombo, Boolean is3dCustomizable, Pageable pageable) {
        Page<Product> productPage;
        if (categoryId != null) {
            productPage = productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable);
        } else {
            productPage = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        }

        return productPage.map(this::mapToSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> productPage = productRepository.searchProducts(keyword, ProductStatus.ACTIVE, pageable);
        return productPage.map(this::mapToSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> get3dCombos() {
        List<Product> combos = productRepository.findByIsComboTrueAndStatus(ProductStatus.ACTIVE);
        return combos.stream().map(this::mapToSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm với slug: " + slug));

        return buildProductDetail(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Không tìm thấy sản phẩm với id: " + id));

        return buildProductDetail(product);
    }

    private ProductSummaryResponse mapToSummary(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        String thumbnailUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .supplierId(product.getSupplierId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .shortDescription(product.getShortDescription())
                .basePrice(product.getBasePrice())
                .isCombo(product.getIsCombo())
                .is3dCustomizable(product.getIs3dCustomizable())
                .isLivestock(product.getIsLivestock())
                .isFragileGlass(product.getIsFragileGlass())
                .status(product.getStatus())
                .totalSales(product.getTotalSales())
                .rating(product.getRating())
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private ProductDetailResponse buildProductDetail(Product product) {
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId());
        List<ProductVariantResponse> variantResponses = variants.stream()
                .map(v -> ProductVariantResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .name(v.getName())
                        .price(v.getPrice())
                        .originalPrice(v.getOriginalPrice())
                        .weightGrams(v.getWeightGrams())
                        .dimensionsCm(v.getDimensionsCm())
                        .attributes(v.getAttributes())
                        .build())
                .collect(Collectors.toList());

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductImageResponse> imageResponses = images.stream()
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isThumbnail(img.getIsThumbnail())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .supplierId(product.getSupplierId())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .isCombo(product.getIsCombo())
                .is3dCustomizable(product.getIs3dCustomizable())
                .isLivestock(product.getIsLivestock())
                .isFragileGlass(product.getIsFragileGlass())
                .status(product.getStatus())
                .totalSales(product.getTotalSales())
                .rating(product.getRating())
                .createdAt(product.getCreatedAt())
                .variants(variantResponses)
                .images(imageResponses)
                .build();
    }
}
