package com.aquarium.aquarium3d.service;

import com.aquarium.aquarium3d.dto.SaveDesignRequest;
import com.aquarium.aquarium3d.dto.UserDesignResponse;
import com.aquarium.common.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface UserDesignService {
    UserDesignResponse saveDesign(UUID ownerId, SaveDesignRequest request);

    /** @param viewer người xem (null nếu ẩn danh) — thiết kế riêng tư chỉ chủ sở hữu/admin xem được */
    UserDesignResponse getDesignByShareSlug(String shareSlug, AuthenticatedUser viewer);

    List<UserDesignResponse> getMyDesigns(UUID ownerId);

    List<UserDesignResponse> getPublicDesignsOfUser(UUID userId);

    List<UserDesignResponse> getPublicDesigns();
}
