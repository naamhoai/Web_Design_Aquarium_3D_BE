package com.aquarium.catalog.repository;

import com.aquarium.catalog.entity.Product;
import com.aquarium.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndStatusAndDeletedAtIsNull(String slug, ProductStatus status);

    Optional<Product> findByIdAndStatusAndDeletedAtIsNull(UUID id, ProductStatus status);


    List<Product> findByIsComboTrueAndStatusAndDeletedAtIsNull(ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.status = :status AND p.deletedAt IS NULL AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\')")
    Page<Product> searchProducts(@Param("keyword") String keyword, @Param("status") ProductStatus status, Pageable pageable);
}
