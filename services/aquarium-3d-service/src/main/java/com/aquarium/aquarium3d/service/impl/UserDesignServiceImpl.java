package com.aquarium.aquarium3d.service.impl;

import com.aquarium.aquarium3d.dto.SaveDesignRequest;
import com.aquarium.aquarium3d.dto.UserDesignResponse;
import com.aquarium.aquarium3d.entity.UserDesign;
import com.aquarium.aquarium3d.repository.UserDesignRepository;
import com.aquarium.aquarium3d.service.UserDesignService;
import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDesignServiceImpl implements UserDesignService {

    private static final Pattern SLUG_FORMAT = Pattern.compile("^design-[a-f0-9]{8,32}$");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_DESIGNS_PER_USER = 200;
    private static final int MAX_QTY_PER_COMPONENT = 99;

    private final UserDesignRepository userDesignRepository;
    private final DesignPricingService pricingService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserDesignResponse saveDesign(UUID ownerId, SaveDesignRequest request) {
        if (userDesignRepository.countByUserId(ownerId) >= MAX_DESIGNS_PER_USER) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Bạn đã lưu tối đa " + MAX_DESIGNS_PER_USER + " bản thiết kế");
        }
        String tankDimensions = requireJsonObject(request.getTankDimensions(), "tankDimensions");
        String sceneData = requireJsonObject(request.getSceneData(), "sceneData");

        Map<String, Integer> quantities = new LinkedHashMap<>();
        request.getComponents().forEach(c -> quantities.merge(c.getSku().trim(), c.getQuantity(), Integer::sum));
        Map<String, DesignPricingService.PricedComponent> priced = pricingService.resolve(quantities.keySet());
        List<String> missing = quantities.keySet().stream().filter(sku -> !priced.containsKey(sku)).toList();
        if (!missing.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                    "Linh kiện không tồn tại hoặc đã ngừng bán: " + String.join(", ", missing.subList(0, Math.min(5, missing.size()))));
        }

        BigDecimal total = BigDecimal.ZERO;
        ObjectNode bom = objectMapper.createObjectNode();
        ArrayNode components = bom.putArray("components");
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            int quantity = entry.getValue();
            if (quantity > MAX_QTY_PER_COMPONENT) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng linh kiện " + entry.getKey() + " vượt quá " + MAX_QTY_PER_COMPONENT);
            }
            DesignPricingService.PricedComponent component = priced.get(entry.getKey());
            BigDecimal lineTotal = component.unitPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);
            ObjectNode node = components.addObject();
            node.put("sku", component.sku());
            node.put("name", component.productName());
            node.put("variantName", component.variantName());
            node.put("unitPrice", component.unitPrice());
            node.put("quantity", quantity);
            node.put("lineTotal", lineTotal);
        }
        bom.put("totalPrice", total);
        bom.put("pricedAt", Instant.now().toString());

        UserDesign design = UserDesign.builder()
                .userId(ownerId)
                .name(request.getName().trim())
                .shareSlug(generateUniqueSlug())
                .thumbnailUrl(request.getThumbnailUrl())
                .tankDimensions(tankDimensions)
                .sceneData(sceneData)
                .bomSnapshot(bom.toString())
                .totalPrice(total)
                .isPublic(request.getIsPublic() == null || request.getIsPublic())
                .viewCount(0)
                .likeCount(0)
                .build();

        UserDesign saved = userDesignRepository.save(design);
        log.info("User {} lưu thiết kế {} (tổng {})", ownerId, saved.getShareSlug(), total);
        return mapToResponse(saved, 0, true);
    }

    @Override
    @Transactional
    public UserDesignResponse getDesignByShareSlug(String shareSlug, AuthenticatedUser viewer) {
        if (shareSlug == null || !SLUG_FORMAT.matcher(shareSlug).matches()) {
            throw notFound();
        }
        UserDesign design = userDesignRepository.findByShareSlug(shareSlug).orElseThrow(UserDesignServiceImpl::notFound);
        boolean owner = viewer != null && viewer.id().equals(design.getUserId());
        boolean admin = viewer != null && viewer.isAdmin();
        if (!Boolean.TRUE.equals(design.getIsPublic()) && !owner && !admin) {
            throw notFound(); // 404 thay vì 403 để không lộ sự tồn tại của thiết kế riêng tư
        }
        int extraView = 0;
        if (!owner) {
            userDesignRepository.incrementViewCount(design.getId());
            extraView = 1;
        }
        return mapToResponse(design, extraView, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDesignResponse> getMyDesigns(UUID ownerId) {
        return userDesignRepository.findTop100ByUserIdOrderByCreatedAtDesc(ownerId).stream()
                .map(d -> mapToResponse(d, 0, true))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDesignResponse> getPublicDesignsOfUser(UUID userId) {
        return userDesignRepository.findTop50ByUserIdAndIsPublicTrueOrderByCreatedAtDesc(userId).stream()
                .map(d -> mapToResponse(d, 0, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDesignResponse> getPublicDesigns() {
        return userDesignRepository.findTop50ByIsPublicTrueOrderByLikeCountDescCreatedAtDesc().stream()
                .map(d -> mapToResponse(d, 0, false))
                .collect(Collectors.toList());
    }

    private String requireJsonObject(String raw, String field) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node == null || !node.isObject()) {
                throw new AppException(ErrorCode.BAD_REQUEST, field + " phải là một JSON object");
            }
            return node.toString();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.BAD_REQUEST, field + " không phải JSON hợp lệ");
        }
    }

    private String generateUniqueSlug() {
        for (int attempt = 0; attempt < 5; attempt++) {
            byte[] bytes = new byte[8];
            RANDOM.nextBytes(bytes);
            String slug = "design-" + HexFormat.of().formatHex(bytes);
            if (!userDesignRepository.existsByShareSlug(slug)) {
                return slug;
            }
        }
        throw new AppException(ErrorCode.CONFLICT, "Không thể tạo mã chia sẻ, vui lòng thử lại");
    }

    private static AppException notFound() {
        return new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thiết kế phối cảnh 3D với mã chia sẻ này");
    }

    private UserDesignResponse mapToResponse(UserDesign design, int extraViews, boolean includeScene) {
        return UserDesignResponse.builder()
                .id(design.getId())
                .userId(design.getUserId())
                .name(design.getName())
                .shareSlug(design.getShareSlug())
                .shareUrl("/studio?design=" + design.getShareSlug())
                .thumbnailUrl(design.getThumbnailUrl())
                .tankDimensions(includeScene ? design.getTankDimensions() : null)
                .sceneData(includeScene ? design.getSceneData() : null)
                .bomSnapshot(includeScene ? design.getBomSnapshot() : null)
                .totalPrice(design.getTotalPrice())
                .isPublic(design.getIsPublic())
                .viewCount(design.getViewCount() + extraViews)
                .likeCount(design.getLikeCount())
                .createdAt(design.getCreatedAt())
                .build();
    }
}
