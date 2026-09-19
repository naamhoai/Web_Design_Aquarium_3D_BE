package com.aquarium.aquarium3d.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityCheckRequest {

    @NotEmpty(message = "Danh sách loài cá/tép không được để trống")
    private List<Integer> speciesIds;

    @Positive(message = "Thể tích bể nước phải lớn hơn 0 lít")
    private Double tankVolumeLiters;
}
