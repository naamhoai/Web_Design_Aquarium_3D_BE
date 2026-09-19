package com.aquarium.order.repository;

import com.aquarium.order.entity.OrderShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderShipmentRepository extends JpaRepository<OrderShipment, UUID> {
    List<OrderShipment> findBySubOrderId(UUID subOrderId);
    Optional<OrderShipment> findByTrackingNumber(String trackingNumber);
}
