package com.aquarium.inventory.repository;

import com.aquarium.inventory.entity.LivestockQuarantine;
import com.aquarium.inventory.entity.QuarantineStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LivestockQuarantineRepository extends JpaRepository<LivestockQuarantine, UUID> {
    List<LivestockQuarantine> findByStatus(QuarantineStatus status);
    List<LivestockQuarantine> findByInventoryItemId(UUID inventoryItemId);
    List<LivestockQuarantine> findByInventoryItemIdIn(Collection<UUID> inventoryItemIds);
    List<LivestockQuarantine> findByInventoryItemIdInAndStatus(Collection<UUID> inventoryItemIds, QuarantineStatus status);
    Optional<LivestockQuarantine> findByBatchCode(String batchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM LivestockQuarantine q WHERE q.id = :id")
    Optional<LivestockQuarantine> lockById(@Param("id") UUID id);
}
