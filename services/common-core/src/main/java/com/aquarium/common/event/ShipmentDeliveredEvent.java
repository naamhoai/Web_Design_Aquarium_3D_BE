package com.aquarium.common.event;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDeliveredEvent implements Serializable {
    private UUID shipmentId;
    private UUID subOrderId;
    private String trackingNumber;
    private UUID customerId;
    private boolean isExpressLive2h;
    private Instant deliveredAt;
    private Instant doaClaimDeadline; // deliveredAt + 2 hours
}
