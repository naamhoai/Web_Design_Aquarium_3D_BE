package com.aquarium.aquarium3d.repository;

import com.aquarium.aquarium3d.entity.SpeciesCompatibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpeciesCompatibilityRepository extends JpaRepository<SpeciesCompatibility, Integer> {
    @Query("SELECT sc FROM SpeciesCompatibility sc WHERE " +
           "(sc.speciesAId = :speciesA AND sc.speciesBId = :speciesB) OR " +
           "(sc.speciesAId = :speciesB AND sc.speciesBId = :speciesA)")
    Optional<SpeciesCompatibility> findCompatibility(@Param("speciesA") Integer speciesA, @Param("speciesB") Integer speciesB);
}
