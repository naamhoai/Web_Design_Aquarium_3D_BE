package com.aquarium.catalog.repository;

import com.aquarium.catalog.entity.Product;
import com.aquarium.catalog.entity.ProductStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bộ lọc danh sách sản phẩm công khai. Dùng Criteria API thay cho JPQL có tham số null
 * (PostgreSQL không suy ra được kiểu của tham số chuỗi null trong "(:x IS NULL OR ...)").
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public record Filter(Integer categoryId, Integer excludeCategoryId, Boolean isCombo, Boolean is3dCustomizable,
                         Boolean isLivestock, UUID supplierId, String keyword) {
    }

    public static Specification<Product> visible(Filter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (filter.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), filter.categoryId()));
            }
            if (filter.excludeCategoryId() != null) {
                predicates.add(cb.notEqual(root.get("categoryId"), filter.excludeCategoryId()));
            }
            if (filter.isCombo() != null) {
                predicates.add(cb.equal(root.get("isCombo"), filter.isCombo()));
            }
            if (filter.is3dCustomizable() != null) {
                predicates.add(cb.equal(root.get("is3dCustomizable"), filter.is3dCustomizable()));
            }
            if (filter.isLivestock() != null) {
                predicates.add(cb.equal(root.get("isLivestock"), filter.isLivestock()));
            }
            if (filter.supplierId() != null) {
                predicates.add(cb.equal(root.get("supplierId"), filter.supplierId()));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                // Cả hai vế cùng qua LOWER() của DB để so khớp nhất quán với tiếng Việt có dấu
                Expression<String> pattern = cb.lower(cb.literal("%" + escapeLike(filter.keyword().trim()) + "%"));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("sku")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
