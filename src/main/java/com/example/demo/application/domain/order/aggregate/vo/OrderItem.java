package com.example.demo.application.domain.order.aggregate.vo;

import java.math.BigDecimal;

/**
 * OrderItem - 訂單品項
 * <p>代表訂單中的單一產品及其數量與單價。</p>
 */
public record OrderItem(
    String productId,
    Integer quantity,
    BigDecimal price
) {
    /**
     * 計算此品項的小計
     */
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}