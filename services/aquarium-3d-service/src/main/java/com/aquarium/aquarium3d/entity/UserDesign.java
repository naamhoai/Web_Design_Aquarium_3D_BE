package com.aquarium.aquarium3d.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_designs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "share_slug", nullable = false, unique = true, length = 220)
    private String shareSlug;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tank_dimensions", nullable = false, columnDefinition = "jsonb")
    private String tankDimensions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scene_data", nullable = false, columnDefinition = "jsonb")
    private String sceneData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bom_snapshot", nullable = false, columnDefinition = "jsonb")
    private String bomSnapshot;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
