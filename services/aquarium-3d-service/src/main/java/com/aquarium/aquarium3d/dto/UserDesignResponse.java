package com.aquarium.aquarium3d.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDesignResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String shareSlug;
    private String shareUrl;
    private String thumbnailUrl;
    private String tankDimensions;
    private String sceneData;
    private String bomSnapshot;
    private BigDecimal totalPrice;
    private Boolean isPublic;
    private Integer viewCount;
    private Integer likeCount;
    private Instant createdAt;
}
