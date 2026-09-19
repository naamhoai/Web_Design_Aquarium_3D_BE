package com.aquarium.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Thông điệp lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Chưa xác thực người dùng", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "Bạn không có quyền truy cập chức năng này", HttpStatus.FORBIDDEN),
    USER_NOT_EXISTED(1004, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(1005, "Email hoặc số điện thoại đã được đăng ký", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1006, "Email hoặc mật khẩu không chính xác", HttpStatus.BAD_REQUEST),
    
    // Product & BOM Errors
    PRODUCT_NOT_FOUND(2001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    VARIANT_NOT_FOUND(2002, "Biến thể sản phẩm không tồn tại", HttpStatus.NOT_FOUND),
    BOM_INVALID_LAYER(2003, "Tầng linh kiện bể cá không hợp lệ (Phải từ 1 đến 6)", HttpStatus.BAD_REQUEST),
    COMPONENT_CANNOT_SWAP(2004, "Linh kiện này là cố định, không được phép thay thế", HttpStatus.BAD_REQUEST),
    
    // Biological Rule Errors
    SPECIES_INCOMPATIBLE(2501, "Cảnh báo sinh học: Các loài cá/sinh vật này không thể sống chung!", HttpStatus.BAD_REQUEST),
    TANK_VOLUME_TOO_SMALL(2502, "Thể tích bể quá nhỏ so với số lượng cá hoặc kích thước loài này", HttpStatus.BAD_REQUEST),
    WATER_PARAMETERS_MISMATCH(2503, "Độ pH hoặc nhiệt độ yêu cầu giữa các loài không tương thích", HttpStatus.BAD_REQUEST),
    
    // Inventory Errors
    INVENTORY_OUT_OF_STOCK(3001, "Sản phẩm đã hết hàng tại showroom này", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(3002, "Số lượng tồn kho không đủ để đáp ứng đơn hàng", HttpStatus.BAD_REQUEST),
    LIVESTOCK_IN_QUARANTINE(3003, "Lô cá này đang trong thời gian cách ly kiểm dịch, chưa được phép xuất bán", HttpStatus.BAD_REQUEST),
    
    // Order & DOA Errors
    ORDER_NOT_FOUND(4001, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORDER_CANNOT_CANCEL(4002, "Đơn hàng đã được xuất kho, không thể hủy", HttpStatus.BAD_REQUEST),
    DOA_CLAIM_EXPIRED(4501, "Đã quá thời hạn 2 giờ gửi video unbox bảo hành cá chết (DOA)", HttpStatus.BAD_REQUEST),
    
    // Payment & Escrow
    PAYMENT_FAILED(5001, "Giao dịch thanh toán thất bại", HttpStatus.BAD_REQUEST),
    ESCROW_ALREADY_RELEASED(5002, "Tiền ký quỹ của đơn hàng đã được giải ngân cho nhà cung cấp", HttpStatus.BAD_REQUEST),
    
    // Generic & Domain Specific
    BAD_REQUEST(1007, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1008, "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    SUPPLIER_NOT_FOUND(2101, "Không tìm thấy thông tin nhà cung cấp", HttpStatus.NOT_FOUND),
    WAREHOUSE_NOT_FOUND(2102, "Không tìm thấy kho hàng / showroom chỉ định", HttpStatus.NOT_FOUND),
    INVENTORY_NOT_FOUND(3004, "Không tìm thấy thông tin tồn kho cho mặt hàng này", HttpStatus.NOT_FOUND),
    QUARANTINE_NOT_FOUND(3005, "Không tìm thấy lô kiểm dịch", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
