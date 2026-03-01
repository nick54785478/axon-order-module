package com.example.demo.application.domain.product.event;

import java.math.BigDecimal;

/**
 * ProductUpdatedEvent - 產品資訊已更新事件
 */
public record ProductUpdatedEvent(String productId, String name, BigDecimal price) {
}