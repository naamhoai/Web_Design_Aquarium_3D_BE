package com.aquarium.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cập nhật hồ sơ: trường null = giữ nguyên; chuỗi rỗng ở phone/avatarUrl = xóa giá trị. */
@Getter
@Setter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 1, max = 150, message = "Họ tên phải từ 1 đến 150 ký tự")
    @Pattern(regexp = "^[^<>\"]*$", message = "Họ tên chứa ký tự không hợp lệ")
    private String fullName;

    @Pattern(regexp = "^$|^(\\+84|0)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 2048, message = "Đường dẫn ảnh đại diện quá dài")
    @Pattern(regexp = "^$|^(https://|/)[^\\s\"'<>]*$", message = "Ảnh đại diện phải là đường dẫn https")
    private String avatarUrl;
}
