package com.aquarium.aquarium3d.service.impl;

import com.aquarium.aquarium3d.dto.SaveDesignRequest;
import com.aquarium.aquarium3d.dto.UserDesignResponse;
import com.aquarium.aquarium3d.entity.UserDesign;
import com.aquarium.aquarium3d.repository.UserDesignRepository;
import com.aquarium.aquarium3d.service.UserDesignService;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDesignServiceImpl implements UserDesignService {

    private final UserDesignRepository userDesignRepository;

    @Override
    @Transactional
    public UserDesignResponse saveDesign(SaveDesignRequest request) {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        String shareSlug = "design-" + randomSuffix;

        UserDesign design = UserDesign.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .shareSlug(shareSlug)
                .thumbnailUrl(request.getThumbnailUrl())
                .tankDimensions(request.getTankDimensions())
                .sceneData(request.getSceneData())
                .bomSnapshot(request.getBomSnapshot())
                .totalPrice(request.getTotalPrice())
                .isPublic(true)
                .viewCount(0)
                .likeCount(0)
                .build();

        UserDesign saved = userDesignRepository.save(design);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public UserDesignResponse getDesignByShareSlug(String shareSlug) {
        UserDesign design = userDesignRepository.findByShareSlug(shareSlug)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Không tìm thấy thiết kế phối cảnh 3D với mã chia sẻ này"));

        design.setViewCount(design.getViewCount() + 1);
        userDesignRepository.save(design);

        return mapToResponse(design);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDesignResponse> getUserDesigns(UUID userId) {
        return userDesignRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDesignResponse> getPublicDesigns() {
        return userDesignRepository.findByIsPublicTrueOrderByLikeCountDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private UserDesignResponse mapToResponse(UserDesign design) {
        return UserDesignResponse.builder()
                .id(design.getId())
                .userId(design.getUserId())
                .name(design.getName())
                .shareSlug(design.getShareSlug())
                .shareUrl("/design/share/" + design.getShareSlug())
                .thumbnailUrl(design.getThumbnailUrl())
                .tankDimensions(design.getTankDimensions())
                .sceneData(design.getSceneData())
                .bomSnapshot(design.getBomSnapshot())
                .totalPrice(design.getTotalPrice())
                .isPublic(design.getIsPublic())
                .viewCount(design.getViewCount())
                .likeCount(design.getLikeCount())
                .createdAt(design.getCreatedAt())
                .build();
    }
}
