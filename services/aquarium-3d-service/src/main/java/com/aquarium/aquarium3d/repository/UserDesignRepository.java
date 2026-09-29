package com.aquarium.aquarium3d.repository;

import com.aquarium.aquarium3d.entity.UserDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDesignRepository extends JpaRepository<UserDesign, UUID> {
    Optional<UserDesign> findByShareSlug(String shareSlug);
    boolean existsByShareSlug(String shareSlug);
    long countByUserId(UUID userId);
    List<UserDesign> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
    List<UserDesign> findTop50ByUserIdAndIsPublicTrueOrderByCreatedAtDesc(UUID userId);
    List<UserDesign> findTop50ByIsPublicTrueOrderByLikeCountDescCreatedAtDesc();

    /** Tăng lượt xem nguyên tử (tránh mất lượt khi nhiều người xem cùng lúc). */
    @Modifying
    @Query("UPDATE UserDesign d SET d.viewCount = d.viewCount + 1 WHERE d.id = :id")
    int incrementViewCount(@Param("id") UUID id);
}
