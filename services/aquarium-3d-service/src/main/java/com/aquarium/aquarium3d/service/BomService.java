package com.aquarium.aquarium3d.service;

import com.aquarium.aquarium3d.dto.ExplodedBomResponse;

import java.util.UUID;

public interface BomService {
    ExplodedBomResponse getExplodedBom(UUID parentProductId);
    ExplodedBomResponse getExplodedBomBySku(String sku);
}
