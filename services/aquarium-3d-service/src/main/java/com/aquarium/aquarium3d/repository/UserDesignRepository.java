package com.aquarium.aquarium3d.repository;

import com.aquarium.aquarium3d.entity.UserDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDesignRepository extends JpaRepository<UserDesign, UUID> {
    Optional<UserDesign> findByShareSlug(String shareSlug);
    List<UserDesign> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<UserDesign> findByIsPublicTrueOrderByLikeCountDesc();
}
