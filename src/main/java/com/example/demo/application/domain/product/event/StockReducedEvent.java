package com.example.demo.application.domain.product.event;

/**
 * StockReducedEvent - 庫存已扣減事件
 * <p>
 * 代表庫存已成功保留。若後續支付失敗，此事件可作為逆向「補償庫存」的基準。
 * </p>
 */
public record StockReducedEvent(String productId, String orderId, Integer quantity) {
}