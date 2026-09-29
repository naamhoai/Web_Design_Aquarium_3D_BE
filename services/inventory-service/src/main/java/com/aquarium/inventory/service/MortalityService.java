package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.inventory.dto.MortalityLogRequest;
import com.aquarium.inventory.dto.MortalityLogResponse;
import com.aquarium.inventory.entity.InventoryItem;
import com.aquarium.inventory.entity.InventoryMovement;
import com.aquarium.inventory.entity.MortalityLog;
import com.aquarium.inventory.entity.MovementType;
import com.aquarium.inventory.repository.InventoryItemRepository;
import com.aquarium.inventory.repository.InventoryMovementRepository;
import com.aquarium.inventory.repository.MortalityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MortalityService {

    private final MortalityLogRepository mortalityLogRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository movementRepository;
    private final WarehouseAccess warehouseAccess;

    @Transactional
    public MortalityLogResponse logMortality(AuthenticatedUser user, MortalityLogRequest request) {
        InventoryItem item = inventoryItemRepository.lockById(request.getInventoryItemId())
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        warehouseAccess.assertCanManageWarehouse(user, item.getWarehouseId());

        int currentStock = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
        if (currentStock < request.getDeathCount()) {
            throw new AppException(ErrorCode.BAD_REQUEST, String.format(
                    "Số lượng hao hụt (%d) vượt quá tổng tồn kho hiện có (%d)", request.getDeathCount(), currentStock));
        }

        item.setStockQuantity(currentStock - request.getDeathCount());
        InventoryItem savedItem = inventoryItemRepository.save(item);

        MortalityLog savedLog = mortalityLogRepository.save(MortalityLog.builder()
                .inventoryItemId(item.getId())
                .deathCount(request.getDeathCount())
                .causeOfDeath(request.getCauseOfDeath())
                .temperatureRecorded(request.getTemperatureRecorded())
                .phRecorded(request.getPhRecorded())
                .loggedBy(user.id())
                .build());

        movementRepository.save(InventoryMovement.builder()
                .inventoryItemId(item.getId())
                .movementType(MovementType.MORTALITY_WRITEOFF)
                .quantity(request.getDeathCount())
                .note(String.format("Hao hụt sinh học: %s (Nhiệt độ: %s C, pH: %s)",
                        request.getCauseOfDeath() != null ? request.getCauseOfDeath() : "Chưa rõ nguyên nhân",
                        request.getTemperatureRecorded() != null ? request.getTemperatureRecorded() : "N/A",
                        request.getPhRecorded() != null ? request.getPhRecorded() : "N/A"))
                .createdBy(user.id())
                .build());

        int reserved = savedItem.getReservedQuantity() != null ? savedItem.getReservedQuantity() : 0;
        boolean shortfall = reserved > savedItem.getStockQuantity();
        if (shortfall) {
            // Cá đã chết là sự thật không thể từ chối ghi nhận — cảnh báo để shop xử lý các đơn đang giữ hàng
            log.warn("Kho {} variant {}: lượng đang giữ ({}) vượt tồn kho sau hao hụt ({})",
                    savedItem.getWarehouseId(), savedItem.getProductVariantId(), reserved, savedItem.getStockQuantity());
        }

        MortalityLogResponse response = MortalityLogResponse.fromEntity(savedLog, savedItem.getStockQuantity());
        response.setReservationShortfall(shortfall);
        return response;
    }

    @Transactional(readOnly = true)
    public List<MortalityLogResponse> getMortalityLogsByItem(AuthenticatedUser user, UUID inventoryItemId) {
        InventoryItem item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        warehouseAccess.assertCanManageWarehouse(user, item.getWarehouseId());

        return mortalityLogRepository.findByInventoryItemIdOrderByLoggedAtDesc(inventoryItemId).stream()
                .map(m -> MortalityLogResponse.fromEntity(m, item.getStockQuantity()))
                .collect(Collectors.toList());
    }
}
