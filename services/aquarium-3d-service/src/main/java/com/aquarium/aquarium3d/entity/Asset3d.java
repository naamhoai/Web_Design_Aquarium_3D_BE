package com.aquarium.aquarium3d.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets_3d")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset3d {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "asset_type_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private AssetType type;

    @Column(name = "product_variant_id", unique = true)
    private UUID productVariantId;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(length = 20)
    @Builder.Default
    private String format = "GLB";

    @Column(name = "poly_count")
    private Integer polyCount;

    @Column(name = "bounding_box", columnDefinition = "jsonb")
    private String boundingBox;

    @Column(name = "lod_levels", columnDefinition = "jsonb")
    private String lodLevels;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
