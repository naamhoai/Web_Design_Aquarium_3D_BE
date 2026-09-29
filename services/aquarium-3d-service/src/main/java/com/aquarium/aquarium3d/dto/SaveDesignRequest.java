package com.aquarium.aquarium3d.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu bản thiết kế 3D.
 * - Chủ sở hữu lấy từ JWT (không nhận userId từ client).
 * - Tổng giá do server tính từ {@code components} (SKU + số lượng), không tin giá client gửi.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDesignRequest {

    @NotBlank(message = "Tên bản thiết kế không được để trống")
    @Size(max = 200, message = "Tên bản thiết kế tối đa 200 ký tự")
    private String name;

    @NotBlank(message = "Kích thước bể (JSON) không được để trống")
    @Size(max = 20000, message = "Dữ liệu kích thước bể quá lớn")
    private String tankDimensions;

    @NotBlank(message = "Dữ liệu vị trí các vật thể 3D (JSON) không được để trống")
    @Size(max = 50000, message = "Dữ liệu phối cảnh quá lớn")
    private String sceneData;

    @NotEmpty(message = "Bản thiết kế phải có ít nhất 1 linh kiện")
    @Size(max = 50, message = "Tối đa 50 dòng linh kiện")
    @Valid
    @Builder.Default
    private List<DesignComponent> components = new ArrayList<>();

    @Size(max = 2048, message = "Đường dẫn ảnh quá dài")
    @Pattern(regexp = "^(https://|/)[^\\s\"'<>]*$", message = "Ảnh thu nhỏ phải là đường dẫn https hoặc đường dẫn nội bộ")
    private String thumbnailUrl;

    private Boolean isPublic;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignComponent {
        @NotBlank(message = "Thiếu mã SKU")
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Mã SKU không hợp lệ")
        private String sku;

        @NotNull
        @Min(1)
        @Max(99)
        private Integer quantity;
    }
}
