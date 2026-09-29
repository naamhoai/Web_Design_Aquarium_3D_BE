package com.aquarium.supplier.service;

import com.aquarium.common.exception.AppException;
import com.aquarium.common.exception.ErrorCode;
import com.aquarium.common.security.AuthenticatedUser;
import com.aquarium.supplier.dto.RegisterSupplierRequest;
import com.aquarium.supplier.dto.SupplierResponse;
import com.aquarium.supplier.dto.UpdateSupplierRequest;
import com.aquarium.supplier.entity.Supplier;
import com.aquarium.supplier.entity.SupplierStatus;
import com.aquarium.supplier.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final BigDecimal MAX_COMMISSION = new BigDecimal("50.00");

    private final SupplierRepository supplierRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public SupplierResponse registerSupplier(AuthenticatedUser user, RegisterSupplierRequest request) {
        if (supplierRepository.findByUserId(user.id()).isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tài khoản này đã đăng ký làm nhà cung cấp");
        }
        if (supplierRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Slug đã được sử dụng bởi nhà cung cấp khác");
        }

        Supplier saved = supplierRepository.save(Supplier.builder()
                .userId(user.id())
                .storeName(request.getStoreName().trim())
                .slug(request.getSlug())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .businessLicense(request.getBusinessLicense())
                .taxCode(blankToNull(request.getTaxCode()))
                .rating(BigDecimal.valueOf(5.00))
                .reviewCount(0)
                .commissionRate(BigDecimal.valueOf(8.00))
                .status(SupplierStatus.PENDING)   // luôn chờ admin duyệt
                .build());
        log.info("User {} đăng ký gian hàng {} (id: {})", user.id(), saved.getStoreName(), saved.getId());
        return SupplierResponse.fromEntity(saved);
    }

    /** Trang gian hàng công khai: chỉ hiện shop ACTIVE; chủ shop/admin xem được mọi trạng thái. */
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierByIdOrSlug(String idOrSlug, AuthenticatedUser viewer) {
        Supplier supplier;
        try {
            UUID id = UUID.fromString(idOrSlug);
            supplier = supplierRepository.findById(id).orElse(null);
        } catch (IllegalArgumentException e) {
            supplier = supplierRepository.findBySlug(idOrSlug).orElse(null);
        }
        if (supplier == null || supplier.getDeletedAt() != null) {
            throw new AppException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        boolean privileged = isOwnerOrAdmin(viewer, supplier);
        if (supplier.getStatus() != SupplierStatus.ACTIVE && !privileged) {
            throw new AppException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        return SupplierResponse.fromEntity(supplier, privileged);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getMySupplier(AuthenticatedUser user) {
        Supplier supplier = supplierRepository.findByUserId(user.id())
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND, "Bạn chưa có hồ sơ nhà cung cấp"));
        return SupplierResponse.fromEntity(supplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(AuthenticatedUser user, UUID id, UpdateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .filter(s -> isOwnerOrAdmin(user, s))
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));

        if (request.getStoreName() != null) supplier.setStoreName(request.getStoreName().trim());
        if (request.getDescription() != null) supplier.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) supplier.setLogoUrl(request.getLogoUrl());
        if (request.getBannerUrl() != null) supplier.setBannerUrl(request.getBannerUrl());
        if (request.getBusinessLicense() != null) supplier.setBusinessLicense(request.getBusinessLicense());
        if (request.getTaxCode() != null) supplier.setTaxCode(blankToNull(request.getTaxCode()));

        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers(SupplierStatus status) {
        List<Supplier> suppliers = status != null ? supplierRepository.findAllByStatus(status) : supplierRepository.findAll();
        return suppliers.stream().map(SupplierResponse::fromEntity).collect(Collectors.toList());
    }

    /** Chỉ admin (được kiểm tra ở controller bằng @PreAuthorize). */
    @Transactional
    public SupplierResponse updateSupplierStatus(AuthenticatedUser admin, UUID id, SupplierStatus status, BigDecimal commissionRate) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));

        if (commissionRate != null) {
            if (commissionRate.signum() < 0 || commissionRate.compareTo(MAX_COMMISSION) > 0) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Tỷ lệ hoa hồng phải trong khoảng 0 - 50%");
            }
            supplier.setCommissionRate(commissionRate);
        }
        supplier.setStatus(status);
        Supplier saved = supplierRepository.save(supplier);

        if (status == SupplierStatus.ACTIVE) {
            // Nâng vai trò tài khoản chủ shop lên SUPPLIER (có hiệu lực ở lần làm mới token kế tiếp)
            int updated = jdbcTemplate.update(
                    "UPDATE users SET role = 'SUPPLIER' WHERE id = ? AND role = 'CUSTOMER'", supplier.getUserId());
            if (updated > 0) {
                log.info("Nâng vai trò user {} lên SUPPLIER", supplier.getUserId());
            }
        }
        log.info("Admin {} đổi trạng thái gian hàng {} -> {}", admin.id(), saved.getStoreName(), status);
        return SupplierResponse.fromEntity(saved);
    }

    public static boolean isOwnerOrAdmin(AuthenticatedUser user, Supplier supplier) {
        return user != null && (user.isAdmin() || supplier.getUserId().equals(user.id()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
