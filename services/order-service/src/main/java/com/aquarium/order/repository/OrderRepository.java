package com.aquarium.order.repository;

import com.aquarium.order.entity.Order;
import com.aquarium.order.entity.OrderStatus;
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
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    boolean existsByOrderNumber(String orderNumber);

    /** Khóa đơn hàng khi đổi trạng thái để tránh hai thao tác hủy/xử lý chạy chồng nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);
}
