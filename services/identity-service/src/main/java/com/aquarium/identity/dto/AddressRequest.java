package com.aquarium.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Địa chỉ nhận hàng trong sổ địa chỉ của chính người dùng (chủ sở hữu lấy từ JWT). */
@Getter
@Setter
@NoArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 150, message = "Tên người nhận tối đa 150 ký tự")
    private String recipientName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String addressLine;

    @Size(max = 100, message = "Phường/xã tối đa 100 ký tự")
    private String ward;

    @NotBlank(message = "Vui lòng nhập quận/huyện")
    @Size(max = 100, message = "Quận/huyện tối đa 100 ký tự")
    private String district;

    @NotBlank(message = "Vui lòng nhập tỉnh/thành phố")
    @Size(max = 100, message = "Tỉnh/thành phố tối đa 100 ký tự")
    private String city;

    /** true => đặt làm địa chỉ mặc định. */
    private Boolean isDefault;
}
