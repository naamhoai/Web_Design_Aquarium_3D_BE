package com.aquarium.order.repository;

import com.aquarium.order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    Optional<CartItem> findByCartIdAndProductVariantIdAndUserDesignId(UUID cartId, UUID productVariantId, UUID userDesignId);
    Optional<CartItem> findByCartIdAndProductVariantIdAndUserDesignIdIsNull(UUID cartId, UUID productVariantId);
    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
    long countByCartId(UUID cartId);
    void deleteByCartId(UUID cartId);
}
