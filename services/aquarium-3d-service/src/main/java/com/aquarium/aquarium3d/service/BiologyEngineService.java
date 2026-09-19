package com.aquarium.aquarium3d.service;

import com.aquarium.aquarium3d.dto.CompatibilityCheckRequest;
import com.aquarium.aquarium3d.dto.CompatibilityCheckResponse;
import com.aquarium.aquarium3d.entity.BiologicalRule;

import java.util.List;

public interface BiologyEngineService {
    List<BiologicalRule> getAllRules();
    CompatibilityCheckResponse checkCompatibility(CompatibilityCheckRequest request);
}
