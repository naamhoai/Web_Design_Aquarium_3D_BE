package com.aquarium.order.repository;

import com.aquarium.order.entity.SubOrder;
import com.aquarium.order.entity.SubOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubOrderRepository extends JpaRepository<SubOrder, UUID> {
    List<SubOrder> findByOrderId(UUID orderId);
    List<SubOrder> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
    List<SubOrder> findBySupplierIdAndStatusOrderByCreatedAtDesc(UUID supplierId, SubOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SubOrder s WHERE s.id = :id")
    Optional<SubOrder> findByIdForUpdate(@Param("id") UUID id);

    /** Chỉ lấy id đơn cha (không nạp entity) để có thể khóa đơn cha trước. */
    @Query("SELECT s.orderId FROM SubOrder s WHERE s.id = :id")
    Optional<UUID> findOrderIdById(@Param("id") UUID id);
}
