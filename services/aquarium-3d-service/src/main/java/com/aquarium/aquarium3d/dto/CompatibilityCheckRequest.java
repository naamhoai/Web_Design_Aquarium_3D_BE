package com.aquarium.aquarium3d.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityCheckRequest {

    @NotEmpty(message = "Danh sách loài cá/tép không được để trống")
    @Size(max = 30, message = "Tối đa 30 loài trong một lần kiểm tra")
    private List<@NotNull Integer> speciesIds;

    @NotNull(message = "Thể tích bể không được để trống")
    @Positive(message = "Thể tích bể nước phải lớn hơn 0 lít")
    @DecimalMax(value = "100000", message = "Thể tích bể quá lớn")
    private Double tankVolumeLiters;

    /** Số lượng cá thể theo id loài (tùy chọn). Không truyền => dùng số lượng tối thiểu để nuôi đàn. */
    private Map<Integer, @Min(1) @Max(1000) Integer> quantities;
}
