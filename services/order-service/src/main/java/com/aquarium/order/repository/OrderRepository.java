package com.aquarium.order.repository;

import com.aquarium.order.entity.Order;
import com.aquarium.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
