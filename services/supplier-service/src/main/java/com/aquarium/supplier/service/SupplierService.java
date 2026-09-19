package com.aquarium.supplier.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.supplier.dto.RegisterSupplierRequest;
import com.aquarium.supplier.dto.SupplierResponse;
import com.aquarium.supplier.dto.UpdateSupplierRequest;
import com.aquarium.supplier.entity.Supplier;
import com.aquarium.supplier.entity.SupplierStatus;
import com.aquarium.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional
    public SupplierResponse registerSupplier(RegisterSupplierRequest request) {
        if (supplierRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Slug đã được sử dụng bởi nhà cung cấp khác: " + request.getSlug());
        }

        if (supplierRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tài khoản người dùng này đã đăng ký làm nhà cung cấp");
        }

        Supplier supplier = Supplier.builder()
                .userId(request.getUserId())
                .storeName(request.getStoreName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .businessLicense(request.getBusinessLicense())
                .taxCode(request.getTaxCode())
                .rating(BigDecimal.valueOf(5.00))
                .reviewCount(0)
                .commissionRate(BigDecimal.valueOf(8.00))
                .status(SupplierStatus.PENDING)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Registered new supplier store: {} (id: {})", saved.getStoreName(), saved.getId());
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByIdOrSlug(String idOrSlug) {
        Supplier supplier;
        try {
            UUID id = UUID.fromString(idOrSlug);
            supplier = supplierRepository.findById(id)
                    .orElseGet(() -> supplierRepository.findBySlug(idOrSlug).orElse(null));
        } catch (IllegalArgumentException e) {
            supplier = supplierRepository.findBySlug(idOrSlug).orElse(null);
        }

        if (supplier == null) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhà cung cấp: " + idOrSlug);
        }
        return SupplierResponse.fromEntity(supplier);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByUserId(UUID userId) {
        Supplier supplier = supplierRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Người dùng chưa có hồ sơ nhà cung cấp"));
        return SupplierResponse.fromEntity(supplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(UUID id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhà cung cấp"));

        if (request.getStoreName() != null) supplier.setStoreName(request.getStoreName());
        if (request.getDescription() != null) supplier.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) supplier.setLogoUrl(request.getLogoUrl());
        if (request.getBannerUrl() != null) supplier.setBannerUrl(request.getBannerUrl());
        if (request.getBusinessLicense() != null) supplier.setBusinessLicense(request.getBusinessLicense());
        if (request.getTaxCode() != null) supplier.setTaxCode(request.getTaxCode());

        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers(SupplierStatus status) {
        List<Supplier> suppliers = (status != null)
                ? supplierRepository.findAllByStatus(status)
                : supplierRepository.findAll();

        return suppliers.stream()
                .map(SupplierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierResponse updateSupplierStatus(UUID id, SupplierStatus status, BigDecimal commissionRate) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhà cung cấp"));

        supplier.setStatus(status);
        if (commissionRate != null) {
            supplier.setCommissionRate(commissionRate);
        }

        Supplier saved = supplierRepository.save(supplier);
        log.info("Admin updated supplier status: {} -> {}", saved.getStoreName(), status);
        return SupplierResponse.fromEntity(saved);
    }
}
