package com.aquarium.inventory.repository;

import com.aquarium.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByWarehouseIdAndProductVariantId(UUID warehouseId, UUID productVariantId);
    List<InventoryItem> findByProductVariantId(UUID productVariantId);
    List<InventoryItem> findByWarehouseId(UUID warehouseId);

    @Query("SELECT i FROM InventoryItem i WHERE (i.stockQuantity - i.reservedQuantity) <= i.lowStockThreshold")
    List<InventoryItem> findLowStockItems();
}
