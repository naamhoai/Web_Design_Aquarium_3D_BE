package com.aquarium.catalog.repository;

import com.aquarium.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

    /** Thứ tự (createdAt, sku) trùng với quy tắc chọn biến thể mặc định ở order-service. */
    List<ProductVariant> findByProductIdInAndIsActiveTrueOrderByCreatedAtAscSkuAsc(Collection<UUID> productIds);

    Optional<ProductVariant> findBySku(String sku);
}
