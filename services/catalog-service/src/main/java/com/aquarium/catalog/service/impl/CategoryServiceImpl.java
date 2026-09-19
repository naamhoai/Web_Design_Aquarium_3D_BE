package com.aquarium.catalog.service.impl;

import com.aquarium.catalog.dto.CategoryResponse;
import com.aquarium.catalog.entity.Category;
import com.aquarium.catalog.repository.CategoryRepository;
import com.aquarium.catalog.service.CategoryService;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIdIsNullAndIsActiveTrue();
        return rootCategories.stream()
                .map(this::mapCategoryToTree)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Danh mục không tồn tại"));
        return mapToResponse(category);
    }

    private CategoryResponse mapCategoryToTree(Category parent) {
        List<Category> children = categoryRepository.findByParentIdAndIsActiveTrue(parent.getId());
        List<CategoryResponse> childResponses = children.stream()
                .map(this::mapCategoryToTree)
                .collect(Collectors.toList());

        return CategoryResponse.builder()
                .id(parent.getId())
                .parentId(parent.getParentId())
                .name(parent.getName())
                .slug(parent.getSlug())
                .description(parent.getDescription())
                .iconUrl(parent.getIconUrl())
                .level(parent.getLevel())
                .children(childResponses)
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .level(category.getLevel())
                .build();
    }
}
