package com.example.demo.application.domain.order.event;

/**
 * OrderCancelledEvent - 表示訂單已被取消
 */
public record OrderCancelledEvent(String orderId) {
}