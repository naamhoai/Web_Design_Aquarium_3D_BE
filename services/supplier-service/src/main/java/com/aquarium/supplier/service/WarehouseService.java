package com.aquarium.supplier.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.supplier.dto.CreateWarehouseRequest;
import com.aquarium.supplier.dto.WarehouseResponse;
import com.aquarium.supplier.entity.Warehouse;
import com.aquarium.supplier.repository.SupplierRepository;
import com.aquarium.supplier.repository.WarehouseRepository;
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
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public WarehouseResponse createWarehouse(UUID supplierId, CreateWarehouseRequest request) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhà cung cấp: " + supplierId);
        }

        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã kho đã tồn tại: " + request.getCode());
        }

        Warehouse warehouse = Warehouse.builder()
                .supplierId(supplierId)
                .name(request.getName())
                .code(request.getCode())
                .warehouseType(request.getWarehouseType())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactPhone(request.getContactPhone())
                .isActive(true)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Created warehouse {} ({}) for supplier {}", saved.getName(), saved.getCode(), supplierId);
        return WarehouseResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehousesBySupplier(UUID supplierId) {
        return warehouseRepository.findBySupplierId(supplierId).stream()
                .map(WarehouseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy kho: " + id));
        return WarehouseResponse.fromEntity(warehouse);
    }

    @Transactional
    public WarehouseResponse toggleWarehouseStatus(UUID id, boolean active) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy kho: " + id));
        warehouse.setIsActive(active);
        return WarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }
}
