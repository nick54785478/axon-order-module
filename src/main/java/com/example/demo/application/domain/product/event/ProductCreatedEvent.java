package com.example.demo.application.domain.product.event;

import java.math.BigDecimal;

/**
 * ProductCreatedEvent - 產品已建立事件
 * <p>
 * 代表產品已成功定義於系統中。此事件將用於重建產品狀態，並同步至查詢端資料庫。
 * </p>
 */
public record ProductCreatedEvent(String productId, String name, BigDecimal price, Integer initialStock) {
}