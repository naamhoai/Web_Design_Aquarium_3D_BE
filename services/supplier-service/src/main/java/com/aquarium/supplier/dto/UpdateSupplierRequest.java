package com.aquarium.supplier.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Chủ shop chỉ được sửa thông tin hồ sơ — không thể tự đổi trạng thái hay hoa hồng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {

    @Size(min = 1, max = 200, message = "storeName phải từ 1 đến 200 ký tự")
    private String storeName;

    @Size(max = 2000, message = "description is too long")
    private String description;

    @Size(max = 2048)
    @Pattern(regexp = "^(https://|/)[^\\s\"'<>]*$", message = "logoUrl phải là đường dẫn https hoặc đường dẫn nội bộ")
    private String logoUrl;

    @Size(max = 2048)
    @Pattern(regexp = "^(https://|/)[^\\s\"'<>]*$", message = "bannerUrl phải là đường dẫn https hoặc đường dẫn nội bộ")
    private String bannerUrl;

    @Size(max = 100, message = "businessLicense is too long")
    private String businessLicense;

    @Pattern(regexp = "^$|^[0-9-]{10,14}$", message = "Mã số thuế không hợp lệ")
    private String taxCode;
}
