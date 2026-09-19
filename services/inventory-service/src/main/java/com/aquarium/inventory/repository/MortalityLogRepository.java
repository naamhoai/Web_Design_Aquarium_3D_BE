package com.aquarium.inventory.repository;

import com.aquarium.inventory.entity.MortalityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MortalityLogRepository extends JpaRepository<MortalityLog, Long> {
    List<MortalityLog> findByInventoryItemIdOrderByLoggedAtDesc(UUID inventoryItemId);
}
