package com.aquarium.order.repository;

import com.aquarium.order.entity.SubOrder;
import com.aquarium.order.entity.SubOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubOrderRepository extends JpaRepository<SubOrder, UUID> {
    List<SubOrder> findByOrderId(UUID orderId);
    List<SubOrder> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
    List<SubOrder> findBySupplierIdAndStatusOrderByCreatedAtDesc(UUID supplierId, SubOrderStatus status);
}
