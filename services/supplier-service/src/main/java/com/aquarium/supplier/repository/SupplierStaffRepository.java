package com.aquarium.supplier.repository;

import com.aquarium.supplier.entity.SupplierStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierStaffRepository extends JpaRepository<SupplierStaff, UUID> {
    List<SupplierStaff> findBySupplierId(UUID supplierId);
    Optional<SupplierStaff> findBySupplierIdAndUserId(UUID supplierId, UUID userId);
}
