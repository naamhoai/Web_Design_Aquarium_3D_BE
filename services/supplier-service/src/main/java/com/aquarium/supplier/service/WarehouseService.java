package com.aquarium.supplier.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.supplier.dto.CreateWarehouseRequest;
import com.aquarium.supplier.dto.WarehouseResponse;
import com.aquarium.supplier.entity.Supplier;
import com.aquarium.supplier.entity.SupplierStatus;
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
    public WarehouseResponse createWarehouse(AuthenticatedUser user, UUID supplierId, CreateWarehouseRequest request) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(s -> SupplierService.isOwnerOrAdmin(user, s))
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
        if (!user.isAdmin() && supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Gian hàng chưa được duyệt, chưa thể tạo kho");
        }
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mã kho đã tồn tại");
        }

        Warehouse saved = warehouseRepository.save(Warehouse.builder()
                .supplierId(supplierId)
                .name(request.getName().trim())
                .code(request.getCode())
                .warehouseType(request.getWarehouseType())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .district(request.getDistrict().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .contactPhone(request.getContactPhone() == null || request.getContactPhone().isBlank() ? null : request.getContactPhone())
                .isActive(true)
                .build());
        log.info("User {} tạo kho {} ({}) cho nhà cung cấp {}", user.id(), saved.getName(), saved.getCode(), supplierId);
        return WarehouseResponse.fromEntity(saved);
    }

    /** Khách xem được các kho/showroom đang hoạt động của shop ACTIVE; chủ shop/admin xem tất cả. */
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehousesBySupplier(UUID supplierId, AuthenticatedUser viewer) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
        boolean privileged = SupplierService.isOwnerOrAdmin(viewer, supplier);
        if (!privileged && supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new AppException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        return warehouseRepository.findBySupplierId(supplierId).stream()
                .filter(w -> privileged || Boolean.TRUE.equals(w.getIsActive()))
                .map(WarehouseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(UUID id, AuthenticatedUser viewer) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Supplier supplier = supplierRepository.findById(warehouse.getSupplierId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        boolean privileged = SupplierService.isOwnerOrAdmin(viewer, supplier);
        boolean publiclyVisible = Boolean.TRUE.equals(warehouse.getIsActive()) && supplier.getStatus() == SupplierStatus.ACTIVE;
        if (!privileged && !publiclyVisible) {
            throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }
        return WarehouseResponse.fromEntity(warehouse);
    }

    @Transactional
    public WarehouseResponse toggleWarehouseStatus(AuthenticatedUser user, UUID id, boolean active) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Supplier supplier = supplierRepository.findById(warehouse.getSupplierId())
                .filter(s -> SupplierService.isOwnerOrAdmin(user, s))
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        warehouse.setIsActive(active);
        log.info("User {} đặt kho {} của {} sang active={}", user.id(), warehouse.getCode(), supplier.getStoreName(), active);
        return WarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }
}
