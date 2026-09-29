package com.aquarium.catalog.service.impl;

import com.aquarium.catalog.dto.*;
import com.aquarium.catalog.entity.*;
import com.aquarium.catalog.repository.*;
import com.aquarium.catalog.service.ProductService;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getProducts(ProductSpecifications.Filter filter, Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(ProductSpecifications.visible(filter), pageable);
        return new PageImpl<>(mapSummaries(productPage.getContent()), pageable, productPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> productPage = productRepository.searchProducts(escapeLike(keyword.trim()), ProductStatus.ACTIVE, pageable);
        return new PageImpl<>(mapSummaries(productPage.getContent()), pageable, productPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> get3dCombos() {
        return mapSummaries(productRepository.findByIsComboTrueAndStatusAndDeletedAtIsNull(ProductStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndStatusAndDeletedAtIsNull(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return buildProductDetail(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID id) {
        Product product = productRepository.findByIdAndStatusAndDeletedAtIsNull(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return buildProductDetail(product);
    }

    /** Nạp biến thể & ảnh theo lô (tránh N+1 query) rồi ghép vào từng sản phẩm. */
    private List<ProductSummaryResponse> mapSummaries(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = products.stream().map(Product::getId).toList();

        Map<UUID, ProductVariant> defaultVariants = new HashMap<>();
        for (ProductVariant variant : productVariantRepository.findByProductIdInAndIsActiveTrueOrderByCreatedAtAscSkuAsc(ids)) {
            defaultVariants.putIfAbsent(variant.getProductId(), variant);
        }

        Map<UUID, String> thumbnails = new HashMap<>();
        Map<UUID, List<ProductImage>> imagesByProduct = productImageRepository.findByProductIdInOrderBySortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));
        imagesByProduct.forEach((productId, images) -> images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                .findFirst()
                .or(() -> images.stream().findFirst())
                .ifPresent(img -> thumbnails.put(productId, img.getImageUrl())));

        return products.stream().map(product -> {
            ProductVariant variant = defaultVariants.get(product.getId());
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
                    .thumbnailUrl(thumbnails.get(product.getId()))
                    .defaultVariantId(variant != null ? variant.getId() : null)
                    .defaultVariantSku(variant != null ? variant.getSku() : null)
                    .price(variant != null ? variant.getPrice() : product.getBasePrice())
                    .originalPrice(variant != null ? variant.getOriginalPrice() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private ProductDetailResponse buildProductDetail(Product product) {
        List<ProductVariantResponse> variantResponses = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId()).stream()
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

        List<ProductImageResponse> imageResponses = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId()).stream()
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

    /** Vô hiệu hóa ký tự đại diện của LIKE trong từ khóa người dùng nhập. */
    private static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
