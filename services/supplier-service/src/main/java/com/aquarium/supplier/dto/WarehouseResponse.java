package com.aquarium.supplier.dto;

import com.aquarium.supplier.entity.Warehouse;
import com.aquarium.supplier.entity.WarehouseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private UUID id;
    private UUID supplierId;
    private String name;
    private String code;
    private WarehouseType warehouseType;
    private String address;
    private String city;
    private String district;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String contactPhone;
    private Boolean isActive;
    private Instant createdAt;

    public static WarehouseResponse fromEntity(Warehouse wh) {
        if (wh == null) return null;
        return WarehouseResponse.builder()
                .id(wh.getId())
                .supplierId(wh.getSupplierId())
                .name(wh.getName())
                .code(wh.getCode())
                .warehouseType(wh.getWarehouseType())
                .address(wh.getAddress())
                .city(wh.getCity())
                .district(wh.getDistrict())
                .latitude(wh.getLatitude())
                .longitude(wh.getLongitude())
                .contactPhone(wh.getContactPhone())
                .isActive(wh.getIsActive())
                .createdAt(wh.getCreatedAt())
                .build();
    }
}
