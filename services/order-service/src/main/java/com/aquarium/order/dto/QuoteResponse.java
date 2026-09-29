package com.aquarium.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Kết quả báo giá — cùng công thức với lúc đặt hàng thật nhưng không giữ hàng, không tạo đơn. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponse {

    private List<Group> groups;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal total;
    /** SKU không tồn tại hoặc đã ngừng bán (không được tính tiền). */
    private List<String> unavailableSkus;
    /** true khi mọi dòng đều bán được và đủ hàng. */
    private boolean orderable;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Group {
        private UUID supplierId;
        private String storeName;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private List<Line> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private UUID productVariantId;
        private UUID productId;
        private String sku;
        private String productName;
        private String variantName;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
        private boolean livestock;
        private boolean fragileGlass;
        /** Số lượng tối đa có thể giữ tại một kho (giống quy tắc khi đặt hàng). */
        private int availableQuantity;
        private boolean inStock;
    }
}
