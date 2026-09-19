package com.aquarium.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSupplierRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotBlank(message = "storeName is required")
    private String storeName;

    @NotBlank(message = "slug is required")
    private String slug;

    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String businessLicense;
    private String taxCode;
}
