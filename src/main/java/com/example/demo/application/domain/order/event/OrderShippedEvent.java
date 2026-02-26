package com.example.demo.application.domain.order.event;

/**
 * OrderShippedEvent - 表示訂單已成功出貨
 */
public record OrderShippedEvent(String orderId) {
}