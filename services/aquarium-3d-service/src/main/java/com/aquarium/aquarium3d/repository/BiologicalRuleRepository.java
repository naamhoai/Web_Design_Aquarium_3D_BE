package com.aquarium.aquarium3d.repository;

import com.aquarium.aquarium3d.entity.BiologicalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BiologicalRuleRepository extends JpaRepository<BiologicalRule, Integer> {
    Optional<BiologicalRule> findByProductId(UUID productId);
    Optional<BiologicalRule> findBySpeciesName(String speciesName);
}
