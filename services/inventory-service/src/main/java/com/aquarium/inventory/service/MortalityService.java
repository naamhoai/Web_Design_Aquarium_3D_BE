package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
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

    @Transactional
    public MortalityLogResponse logMortality(MortalityLogRequest request) {
        InventoryItem item = inventoryItemRepository.findById(request.getInventoryItemId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy mặt hàng trong kho"));

        int currentStock = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
        if (currentStock < request.getDeathCount()) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    String.format("Số lượng hao hụt (%d) vượt quá tổng tồn kho hiện có (%d)",
                            request.getDeathCount(), currentStock));
        }

        // Deduct stock due to mortality
        item.setStockQuantity(currentStock - request.getDeathCount());
        InventoryItem savedItem = inventoryItemRepository.save(item);

        // Record mortality log
        MortalityLog mortalityLog = MortalityLog.builder()
                .inventoryItemId(item.getId())
                .deathCount(request.getDeathCount())
                .causeOfDeath(request.getCauseOfDeath())
                .temperatureRecorded(request.getTemperatureRecorded())
                .phRecorded(request.getPhRecorded())
                .loggedBy(request.getLoggedBy())
                .build();
        MortalityLog savedLog = mortalityLogRepository.save(mortalityLog);

        // Record stock write-off movement
        InventoryMovement movement = InventoryMovement.builder()
                .inventoryItemId(item.getId())
                .movementType(MovementType.MORTALITY_WRITEOFF)
                .quantity(request.getDeathCount())
                .note(String.format("Hao hụt sinh học: %s (Nhiệt độ: %s C, pH: %s)",
                        request.getCauseOfDeath() != null ? request.getCauseOfDeath() : "Chưa rõ nguyên nhân",
                        request.getTemperatureRecorded() != null ? request.getTemperatureRecorded() : "N/A",
                        request.getPhRecorded() != null ? request.getPhRecorded() : "N/A"))
                .createdBy(request.getLoggedBy())
                .build();
        movementRepository.save(movement);

        log.warn("Biological mortality recorded: {} dead for item {}. New stock: {}",
                request.getDeathCount(), item.getId(), savedItem.getStockQuantity());

        return MortalityLogResponse.fromEntity(savedLog, savedItem.getStockQuantity());
    }

    @Transactional(readOnly = true)
    public List<MortalityLogResponse> getMortalityLogsByItem(UUID inventoryItemId) {
        InventoryItem item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy mặt hàng"));

        return mortalityLogRepository.findByInventoryItemIdOrderByLoggedAtDesc(inventoryItemId).stream()
                .map(m -> MortalityLogResponse.fromEntity(m, item.getStockQuantity()))
                .collect(Collectors.toList());
    }
}
