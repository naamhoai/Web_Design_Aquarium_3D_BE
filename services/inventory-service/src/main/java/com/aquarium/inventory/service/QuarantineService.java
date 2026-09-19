package com.aquarium.inventory.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.inventory.dto.QuarantineBatchRequest;
import com.aquarium.inventory.dto.QuarantineBatchResponse;
import com.aquarium.inventory.dto.UpdateQuarantineStatusRequest;
import com.aquarium.inventory.entity.LivestockQuarantine;
import com.aquarium.inventory.entity.QuarantineStatus;
import com.aquarium.inventory.repository.InventoryItemRepository;
import com.aquarium.inventory.repository.LivestockQuarantineRepository;
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
public class QuarantineService {

    private final LivestockQuarantineRepository quarantineRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional
    public QuarantineBatchResponse registerQuarantineBatch(QuarantineBatchRequest request) {
        if (!inventoryItemRepository.existsById(request.getInventoryItemId())) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bản ghi kho tương ứng: " + request.getInventoryItemId());
        }

        if (quarantineRepository.findByBatchCode(request.getBatchCode()).isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã lô kiểm dịch đã tồn tại: " + request.getBatchCode());
        }

        LivestockQuarantine quarantine = LivestockQuarantine.builder()
                .inventoryItemId(request.getInventoryItemId())
                .batchCode(request.getBatchCode())
                .arrivalDate(request.getArrivalDate())
                .quarantineEndDate(request.getQuarantineEndDate())
                .initialQuantity(request.getInitialQuantity())
                .currentHealthyQuantity(request.getInitialQuantity())
                .status(QuarantineStatus.IN_QUARANTINE)
                .inspectionNotes(request.getInspectionNotes())
                .build();

        LivestockQuarantine saved = quarantineRepository.save(quarantine);
        log.info("Registered live fish quarantine batch: {} (qty: {})", saved.getBatchCode(), saved.getInitialQuantity());
        return QuarantineBatchResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<QuarantineBatchResponse> getAllQuarantineBatches(QuarantineStatus status) {
        List<LivestockQuarantine> list = (status != null)
                ? quarantineRepository.findByStatus(status)
                : quarantineRepository.findAll();

        return list.stream()
                .map(QuarantineBatchResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuarantineBatchResponse getQuarantineById(UUID id) {
        LivestockQuarantine q = quarantineRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lô kiểm dịch: " + id));
        return QuarantineBatchResponse.fromEntity(q);
    }

    @Transactional
    public QuarantineBatchResponse updateQuarantineStatus(UUID id, UpdateQuarantineStatusRequest request) {
        LivestockQuarantine q = quarantineRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lô kiểm dịch: " + id));

        q.setStatus(request.getStatus());
        if (request.getCurrentHealthyQuantity() != null) {
            q.setCurrentHealthyQuantity(request.getCurrentHealthyQuantity());
        }
        if (request.getInspectionNotes() != null) {
            q.setInspectionNotes(request.getInspectionNotes());
        }

        LivestockQuarantine saved = quarantineRepository.save(q);
        log.info("Updated quarantine batch {} status to {}", saved.getBatchCode(), saved.getStatus());
        return QuarantineBatchResponse.fromEntity(saved);
    }
}
