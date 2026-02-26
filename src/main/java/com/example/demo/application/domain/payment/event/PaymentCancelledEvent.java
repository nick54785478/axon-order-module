package com.example.demo.application.domain.payment.event;

/**
 * PaymentCancelledEvent - 表示付款已取消
 */
public record PaymentCancelledEvent(String paymentId, String orderId) {
}