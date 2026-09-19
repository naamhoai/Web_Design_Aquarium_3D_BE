package com.aquarium.supplier.repository;

import com.aquarium.supplier.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    List<Warehouse> findBySupplierId(UUID supplierId);
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
}
