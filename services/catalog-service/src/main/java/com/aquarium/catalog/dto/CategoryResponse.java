package com.aquarium.catalog.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Integer id;
    private Integer parentId;
    private String name;
    private String slug;
    private String description;
    private String iconUrl;
    private Integer level;
    private List<CategoryResponse> children;
}
