package com.aquarium.aquarium3d.service;

import com.aquarium.aquarium3d.dto.SaveDesignRequest;
import com.aquarium.aquarium3d.dto.UserDesignResponse;

import java.util.List;
import java.util.UUID;

public interface UserDesignService {
    UserDesignResponse saveDesign(SaveDesignRequest request);
    UserDesignResponse getDesignByShareSlug(String shareSlug);
    List<UserDesignResponse> getUserDesigns(UUID userId);
    List<UserDesignResponse> getPublicDesigns();
}
