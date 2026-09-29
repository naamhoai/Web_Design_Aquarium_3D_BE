package com.aquarium.supplier.dto;

import com.aquarium.supplier.entity.WarehouseType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWarehouseRequest {

    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name is too long")
    private String name;

    @NotBlank(message = "code is required")
    @Pattern(regexp = "^[A-Z0-9-]{3,50}$", message = "code chỉ gồm chữ in hoa, số và dấu gạch ngang (3-50 ký tự)")
    private String code;

    @NotNull(message = "warehouseType is required")
    private WarehouseType warehouseType;

    @NotBlank(message = "address is required")
    @Size(max = 500, message = "address is too long")
    private String address;

    @NotBlank(message = "city is required")
    @Size(max = 100, message = "city is too long")
    private String city;

    @NotBlank(message = "district is required")
    @Size(max = 100, message = "district is too long")
    private String district;

    @DecimalMin(value = "-90.0", message = "latitude out of range")
    @DecimalMax(value = "90.0", message = "latitude out of range")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "longitude out of range")
    @DecimalMax(value = "180.0", message = "longitude out of range")
    private BigDecimal longitude;

    @Pattern(regexp = "^$|^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String contactPhone;
}
