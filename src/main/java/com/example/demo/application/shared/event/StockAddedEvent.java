package com.example.demo.application.shared.event;

/**
 * StockAddedEvent - 庫存已增加事件
 * <p>
 * 代表庫存已還原，用於同步 Read Model。
 * </p>
 */
public record StockAddedEvent(String productId, String orderId, Integer quantity) {
}