package com.aquarium.aquarium3d.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveDesignRequest {

    @NotNull(message = "User ID không được để trống")
    private UUID userId;

    @NotBlank(message = "Tên bản thiết kế không được để trống")
    private String name;

    @NotBlank(message = "Kích thước bể (JSON) không được để trống")
    private String tankDimensions;

    @NotBlank(message = "Dữ liệu vị trí các vật thể 3D (JSON) không được để trống")
    private String sceneData;

    @NotBlank(message = "Ảnh chụp linh kiện BOM (JSON) không được để trống")
    private String bomSnapshot;

    @NotNull(message = "Tổng giá tiền không được để trống")
    private BigDecimal totalPrice;

    private String thumbnailUrl;
}
