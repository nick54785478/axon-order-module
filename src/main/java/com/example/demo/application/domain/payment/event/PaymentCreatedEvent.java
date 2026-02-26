package com.example.demo.application.domain.payment.event;

import java.math.BigDecimal;

/**
 * PaymentCreatedEvent - 表示付款已建立
 */
public record PaymentCreatedEvent(String paymentId, String orderId, BigDecimal amount) {
}