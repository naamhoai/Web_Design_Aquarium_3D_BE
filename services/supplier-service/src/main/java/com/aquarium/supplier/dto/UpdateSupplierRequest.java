package com.aquarium.supplier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {
    private String storeName;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String businessLicense;
    private String taxCode;
}
