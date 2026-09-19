package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.inventory.dto.*;
import com.aquarium.inventory.entity.InventoryItem;
import com.aquarium.inventory.entity.InventoryMovement;
import com.aquarium.inventory.entity.MovementType;
import com.aquarium.inventory.repository.InventoryItemRepository;
import com.aquarium.inventory.repository.InventoryMovementRepository;
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
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public VariantStockSummaryResponse getStockByVariant(UUID productVariantId) {
        List<InventoryItem> items = inventoryItemRepository.findByProductVariantId(productVariantId);

        int totalStock = items.stream().mapToInt(i -> i.getStockQuantity() != null ? i.getStockQuantity() : 0).sum();
        int totalReserved = items.stream().mapToInt(i -> i.getReservedQuantity() != null ? i.getReservedQuantity() : 0).sum();
        int totalAvailable = Math.max(0, totalStock - totalReserved);

        List<InventoryItemResponse> breakdown = items.stream()
                .map(InventoryItemResponse::fromEntity)
                .collect(Collectors.toList());

        return VariantStockSummaryResponse.builder()
                .productVariantId(productVariantId)
                .totalStock(totalStock)
                .totalReserved(totalReserved)
                .totalAvailable(totalAvailable)
                .inStock(totalAvailable > 0)
                .warehouseBreakdown(breakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getStockByWarehouse(UUID warehouseId) {
        return inventoryItemRepository.findByWarehouseId(warehouseId).stream()
                .map(InventoryItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public InventoryItemResponse stockIn(StockInRequest request) {
        InventoryItem item = inventoryItemRepository
                .findByWarehouseIdAndProductVariantId(request.getWarehouseId(), request.getProductVariantId())
                .orElseGet(() -> InventoryItem.builder()
                        .warehouseId(request.getWarehouseId())
                        .productVariantId(request.getProductVariantId())
                        .stockQuantity(0)
                        .reservedQuantity(0)
                        .lowStockThreshold(5)
                        .build());

        item.setStockQuantity(item.getStockQuantity() + request.getQuantity());
        InventoryItem savedItem = inventoryItemRepository.save(item);

        InventoryMovement movement = InventoryMovement.builder()
                .inventoryItemId(savedItem.getId())
                .movementType(MovementType.IMPORT)
                .quantity(request.getQuantity())
                .note(request.getNote() != null ? request.getNote() : "Nhập kho mới")
                .createdBy(request.getCreatedBy())
                .build();
        movementRepository.save(movement);

        log.info("Stocked in {} units for variant {} in warehouse {}",
                request.getQuantity(), request.getProductVariantId(), request.getWarehouseId());
        return InventoryItemResponse.fromEntity(savedItem);
    }

    @Transactional
    public InventoryItemResponse reserveStock(ReserveStockRequest request) {
        InventoryItem item = inventoryItemRepository
                .findByWarehouseIdAndProductVariantId(request.getWarehouseId(), request.getProductVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy mặt hàng trong kho chỉ định"));

        if (item.getAvailableQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    String.format("Không đủ tồn kho khả dụng để giữ hàng. Khả dụng: %d, Yêu cầu: %d",
                            item.getAvailableQuantity(), request.getQuantity()));
        }

        item.setReservedQuantity(item.getReservedQuantity() + request.getQuantity());
        InventoryItem savedItem = inventoryItemRepository.save(item);

        log.info("Reserved {} units of variant {} for order {}",
                request.getQuantity(), request.getProductVariantId(), request.getReferenceOrderId());
        return InventoryItemResponse.fromEntity(savedItem);
    }

    @Transactional
    public InventoryItemResponse releaseStock(ReleaseStockRequest request) {
        InventoryItem item = inventoryItemRepository
                .findByWarehouseIdAndProductVariantId(request.getWarehouseId(), request.getProductVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy mặt hàng trong kho chỉ định"));

        int currentReserved = item.getReservedQuantity() != null ? item.getReservedQuantity() : 0;
        if (currentReserved < request.getQuantity()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng hủy giữ vượt quá số lượng đang bị giữ");
        }

        item.setReservedQuantity(currentReserved - request.getQuantity());
        InventoryItem savedItem = inventoryItemRepository.save(item);

        log.info("Released {} reserved units of variant {} for order {}",
                request.getQuantity(), request.getProductVariantId(), request.getReferenceOrderId());
        return InventoryItemResponse.fromEntity(savedItem);
    }

    @Transactional
    public InventoryItemResponse stockOut(StockOutRequest request) {
        InventoryItem item = inventoryItemRepository
                .findByWarehouseIdAndProductVariantId(request.getWarehouseId(), request.getProductVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy mặt hàng trong kho chỉ định"));

        if (item.getStockQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Số lượng tồn kho không đủ để xuất");
        }

        item.setStockQuantity(item.getStockQuantity() - request.getQuantity());
        int newReserved = Math.max(0, (item.getReservedQuantity() != null ? item.getReservedQuantity() : 0) - request.getQuantity());
        item.setReservedQuantity(newReserved);
        InventoryItem savedItem = inventoryItemRepository.save(item);

        InventoryMovement movement = InventoryMovement.builder()
                .inventoryItemId(savedItem.getId())
                .movementType(MovementType.EXPORT)
                .quantity(request.getQuantity())
                .referenceOrderId(request.getReferenceOrderId())
                .note(request.getNote() != null ? request.getNote() : "Xuất kho giao đơn hàng")
                .createdBy(request.getCreatedBy())
                .build();
        movementRepository.save(movement);

        log.info("Stocked out {} units of variant {} for order {}",
                request.getQuantity(), request.getProductVariantId(), request.getReferenceOrderId());
        return InventoryItemResponse.fromEntity(savedItem);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getLowStockAlerts() {
        return inventoryItemRepository.findLowStockItems().stream()
                .map(InventoryItemResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
