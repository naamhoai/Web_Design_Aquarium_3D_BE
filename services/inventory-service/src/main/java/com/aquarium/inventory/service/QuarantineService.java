package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.inventory.dto.QuarantineBatchRequest;
import com.aquarium.inventory.dto.QuarantineBatchResponse;
import com.aquarium.inventory.dto.UpdateQuarantineStatusRequest;
import com.aquarium.inventory.entity.InventoryItem;
import com.aquarium.inventory.entity.InventoryMovement;
import com.aquarium.inventory.entity.LivestockQuarantine;
import com.aquarium.inventory.entity.MovementType;
import com.aquarium.inventory.entity.QuarantineStatus;
import com.aquarium.inventory.repository.InventoryItemRepository;
import com.aquarium.inventory.repository.InventoryMovementRepository;
import com.aquarium.inventory.repository.LivestockQuarantineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kiểm dịch cá nhập về: lô mới ở trạng thái IN_QUARANTINE và CHƯA được tính vào tồn kho bán được.
 * Khi kết luận PASSED, số cá khỏe mới được cộng vào tồn kho (ghi nhật ký IMPORT).
 * PASSED / FAILED_INFECTED là trạng thái cuối, không thể sửa lại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuarantineService {

    private static final int MAX_QUARANTINE_DAYS = 180;

    private final LivestockQuarantineRepository quarantineRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository movementRepository;
    private final WarehouseAccess warehouseAccess;

    @Transactional
    public QuarantineBatchResponse registerQuarantineBatch(AuthenticatedUser user, QuarantineBatchRequest request) {
        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        warehouseAccess.assertCanManageWarehouse(user, item.getWarehouseId());

        if (request.getQuarantineEndDate().isBefore(request.getArrivalDate())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Ngày kết thúc cách ly phải sau hoặc bằng ngày nhập");
        }
        if (ChronoUnit.DAYS.between(request.getArrivalDate(), request.getQuarantineEndDate()) > MAX_QUARANTINE_DAYS) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Thời gian cách ly tối đa " + MAX_QUARANTINE_DAYS + " ngày");
        }
        if (quarantineRepository.findByBatchCode(request.getBatchCode()).isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã lô kiểm dịch đã tồn tại");
        }

        LivestockQuarantine saved = quarantineRepository.save(LivestockQuarantine.builder()
                .inventoryItemId(request.getInventoryItemId())
                .batchCode(request.getBatchCode())
                .arrivalDate(request.getArrivalDate())
                .quarantineEndDate(request.getQuarantineEndDate())
                .initialQuantity(request.getInitialQuantity())
                .currentHealthyQuantity(request.getInitialQuantity())
                .status(QuarantineStatus.IN_QUARANTINE)
                .inspectionNotes(request.getInspectionNotes())
                .build());
        log.info("Tiếp nhận lô kiểm dịch {} ({} con) bởi {}", saved.getBatchCode(), saved.getInitialQuantity(), user.id());
        return QuarantineBatchResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<QuarantineBatchResponse> getAllQuarantineBatches(AuthenticatedUser user, QuarantineStatus status) {
        List<UUID> warehouseIds = warehouseAccess.manageableWarehouseIds(user);
        List<LivestockQuarantine> list;
        if (warehouseIds == null) {
            list = status != null ? quarantineRepository.findByStatus(status) : quarantineRepository.findAll();
        } else {
            List<UUID> itemIds = warehouseIds.stream()
                    .flatMap(wid -> inventoryItemRepository.findByWarehouseId(wid).stream())
                    .map(InventoryItem::getId)
                    .toList();
            if (itemIds.isEmpty()) {
                return List.of();
            }
            list = status != null
                    ? quarantineRepository.findByInventoryItemIdInAndStatus(itemIds, status)
                    : quarantineRepository.findByInventoryItemIdIn(itemIds);
        }
        return list.stream().map(QuarantineBatchResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuarantineBatchResponse getQuarantineById(AuthenticatedUser user, UUID id) {
        LivestockQuarantine quarantine = quarantineRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUARANTINE_NOT_FOUND));
        assertCanManageBatch(user, quarantine);
        return QuarantineBatchResponse.fromEntity(quarantine);
    }

    @Transactional
    public QuarantineBatchResponse updateQuarantineStatus(AuthenticatedUser user, UUID id, UpdateQuarantineStatusRequest request) {
        LivestockQuarantine quarantine = quarantineRepository.lockById(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUARANTINE_NOT_FOUND));
        assertCanManageBatch(user, quarantine);

        if (quarantine.getStatus() != QuarantineStatus.IN_QUARANTINE) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION, "Lô này đã có kết luận kiểm dịch, không thể thay đổi");
        }
        if (request.getStatus() == QuarantineStatus.IN_QUARANTINE) {
            // Chỉ cập nhật số cá khỏe / ghi chú trong thời gian cách ly
            applyHealthyQuantity(quarantine, request.getCurrentHealthyQuantity());
            if (request.getInspectionNotes() != null) {
                quarantine.setInspectionNotes(request.getInspectionNotes());
            }
            return QuarantineBatchResponse.fromEntity(quarantineRepository.save(quarantine));
        }

        applyHealthyQuantity(quarantine, request.getCurrentHealthyQuantity());
        if (request.getInspectionNotes() != null) {
            quarantine.setInspectionNotes(request.getInspectionNotes());
        }
        quarantine.setStatus(request.getStatus());

        if (request.getStatus() == QuarantineStatus.PASSED && quarantine.getCurrentHealthyQuantity() > 0) {
            InventoryItem item = inventoryItemRepository.lockById(quarantine.getInventoryItemId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
            item.setStockQuantity(item.getStockQuantity() + quarantine.getCurrentHealthyQuantity());
            inventoryItemRepository.save(item);
            movementRepository.save(InventoryMovement.builder()
                    .inventoryItemId(item.getId())
                    .movementType(MovementType.IMPORT)
                    .quantity(quarantine.getCurrentHealthyQuantity())
                    .note("Nhập kho sau kiểm dịch đạt, lô " + quarantine.getBatchCode())
                    .createdBy(user.id())
                    .build());
        }

        LivestockQuarantine saved = quarantineRepository.save(quarantine);
        log.info("Lô kiểm dịch {} kết luận {} ({} con khỏe) bởi {}", saved.getBatchCode(), saved.getStatus(),
                saved.getCurrentHealthyQuantity(), user.id());
        return QuarantineBatchResponse.fromEntity(saved);
    }

    private void applyHealthyQuantity(LivestockQuarantine quarantine, Integer healthy) {
        if (healthy == null) {
            return;
        }
        if (healthy > quarantine.getInitialQuantity()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số cá khỏe không thể lớn hơn số lượng nhập ban đầu");
        }
        quarantine.setCurrentHealthyQuantity(healthy);
    }

    private void assertCanManageBatch(AuthenticatedUser user, LivestockQuarantine quarantine) {
        InventoryItem item = inventoryItemRepository.findById(quarantine.getInventoryItemId())
                .orElseThrow(() -> new AppException(ErrorCode.QUARANTINE_NOT_FOUND));
        try {
            warehouseAccess.assertCanManageWarehouse(user, item.getWarehouseId());
        } catch (AppException e) {
            throw new AppException(ErrorCode.QUARANTINE_NOT_FOUND);
        }
    }
}
