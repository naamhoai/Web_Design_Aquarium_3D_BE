package com.aquarium.aquarium3d.repository;

import com.aquarium.aquarium3d.entity.Asset3d;
import com.aquarium.aquarium3d.entity.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Asset3dRepository extends JpaRepository<Asset3d, UUID> {
    Optional<Asset3d> findByProductVariantId(UUID productVariantId);
    List<Asset3d> findByType(AssetType type);
}
