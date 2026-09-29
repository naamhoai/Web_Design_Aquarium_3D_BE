package com.aquarium.order.dto;

import com.aquarium.order.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Yêu cầu đặt hàng.
 * - Người mua lấy từ JWT (không nhận userId từ client).
 * - Giá, phí vận chuyển, giảm giá đều do server tính — client KHÔNG gửi các giá trị tiền.
 * - {@code items} rỗng => đặt hàng từ giỏ hàng đã lưu trong DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @Valid
    @Size(max = 50, message = "Mỗi đơn hàng tối đa 50 dòng sản phẩm")
    @Builder.Default
    private List<CheckoutItem> items = new ArrayList<>();

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Vui lòng nhập địa chỉ giao hàng")
    @Valid
    private ShippingAddressRequest shippingAddress;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String customerNotes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItem {
        @NotBlank(message = "Thiếu mã SKU")
        @Size(max = 100, message = "Mã SKU quá dài")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Mã SKU không hợp lệ")
        private String sku;

        @NotNull(message = "Thiếu số lượng")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        @Max(value = 99, message = "Số lượng tối đa mỗi sản phẩm là 99")
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressRequest {
        @NotBlank(message = "Vui lòng nhập tên người nhận")
        @Size(max = 150, message = "Tên người nhận tối đa 150 ký tự")
        private String recipientName;

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
        private String phone;

        @NotBlank(message = "Vui lòng nhập địa chỉ")
        @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
        private String addressLine;

        @Size(max = 100)
        private String ward;

        @Size(max = 100)
        private String district;

        @Size(max = 100)
        private String city;
    }
}
