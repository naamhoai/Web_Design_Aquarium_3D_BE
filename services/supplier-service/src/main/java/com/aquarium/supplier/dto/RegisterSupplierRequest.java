package com.aquarium.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Đăng ký làm nhà cung cấp — tài khoản chủ shop là người đang đăng nhập (JWT). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSupplierRequest {

    @NotBlank(message = "storeName is required")
    @Size(max = 200, message = "storeName is too long")
    private String storeName;

    @NotBlank(message = "slug is required")
    @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{1,98}[a-z0-9])$", message = "slug chỉ gồm chữ thường, số và dấu gạch ngang (3-100 ký tự)")
    private String slug;

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
