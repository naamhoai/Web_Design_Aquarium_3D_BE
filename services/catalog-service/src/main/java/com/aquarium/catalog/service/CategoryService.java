package com.aquarium.catalog.service;

import com.aquarium.catalog.dto.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getCategoryTree();
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryBySlug(String slug);
}
