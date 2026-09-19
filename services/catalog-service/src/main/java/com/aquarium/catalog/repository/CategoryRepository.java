package com.aquarium.catalog.repository;

import com.aquarium.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByParentIdIsNullAndIsActiveTrue();
    List<Category> findByParentIdAndIsActiveTrue(Integer parentId);
    List<Category> findAllByIsActiveTrue();
    Optional<Category> findBySlug(String slug);
}
