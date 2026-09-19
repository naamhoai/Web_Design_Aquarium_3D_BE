package com.aquarium.supplier.repository;

import com.aquarium.supplier.entity.Supplier;
import com.aquarium.supplier.entity.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Optional<Supplier> findBySlug(String slug);
    Optional<Supplier> findByUserId(UUID userId);
    boolean existsBySlug(String slug);
    List<Supplier> findAllByStatus(SupplierStatus status);
}
