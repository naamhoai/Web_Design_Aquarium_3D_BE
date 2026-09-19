package com.aquarium.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_shipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_provider", columnDefinition = "shipping_provider_enum")
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private ShippingProvider shippingProvider = ShippingProvider.GHN;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Builder.Default
    @Column(name = "shipping_cost", precision = 15, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_express_live_2h")
    private Boolean isExpressLive2h = false;

    @Builder.Default
    @Column(name = "has_oxygen_tank")
    private Boolean hasOxygenTank = false;

    @Builder.Default
    @Column(name = "fragile_packing_confirmed")
    private Boolean fragilePackingConfirmed = false;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Builder.Default
    @Column(length = 50)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
