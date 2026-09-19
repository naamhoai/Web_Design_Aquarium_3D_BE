package com.aquarium.aquarium3d.repository;

import com.aquarium.aquarium3d.entity.ProductBom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductBomRepository extends JpaRepository<ProductBom, UUID> {
    List<ProductBom> findByParentProductIdOrderByLayerIndexAsc(UUID parentProductId);
}
