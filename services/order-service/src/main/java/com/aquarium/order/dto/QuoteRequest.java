package com.aquarium.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Báo giá giỏ hàng: client chỉ gửi SKU + số lượng, server trả giá thật, phí ship và tình trạng hàng. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {

    @NotEmpty(message = "Giỏ hàng đang trống")
    @Size(max = 50, message = "Mỗi đơn hàng tối đa 50 dòng sản phẩm")
    @Valid
    private List<CheckoutRequest.CheckoutItem> items = new ArrayList<>();
}
