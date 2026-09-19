package com.aquarium.inventory.repository;

import com.aquarium.inventory.entity.LivestockQuarantine;
import com.aquarium.inventory.entity.QuarantineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LivestockQuarantineRepository extends JpaRepository<LivestockQuarantine, UUID> {
    List<LivestockQuarantine> findByStatus(QuarantineStatus status);
    List<LivestockQuarantine> findByInventoryItemId(UUID inventoryItemId);
    Optional<LivestockQuarantine> findByBatchCode(String batchCode);
}
