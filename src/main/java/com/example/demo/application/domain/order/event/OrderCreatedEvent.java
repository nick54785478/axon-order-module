package com.example.demo.application.domain.order.event;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.application.domain.order.aggregate.vo.OrderItem;

/**
 * OrderCreatedEvent - 訂單已建立事件
 * <p>
 * 此事件代表訂單已經成功通過業務校驗並存入系統。 事件溯源 (Event Sourcing) 會透過此事件重建 Order 聚合根的狀態。
 * </p>
 * * @param orderId 訂單 ID
 * 
 * @param items  最終存入訂單的品項明細
 * @param amount 最終計算出的訂單總金額 (以此事件紀錄的金額為準)
 */
public record OrderCreatedEvent(String orderId, List<OrderItem> items, BigDecimal amount) {
}