package com.aquarium.aquarium3d.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_boms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_product_id", nullable = false)
    private UUID parentProductId;

    @Column(name = "component_variant_id", nullable = false)
    private UUID componentVariantId;

    @Column(name = "layer_index", nullable = false)
    private Integer layerIndex; // 1 to 6

    @Column(name = "layer_name", nullable = false, length = 100)
    private String layerName;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;

    @Column(name = "can_swap")
    @Builder.Default
    private Boolean canSwap = true;

    @Column(name = "auto_disassemble_on_stockout")
    @Builder.Default
    private Boolean autoDisassembleOnStockout = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
