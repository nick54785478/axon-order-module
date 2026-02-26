package com.example.demo.application.domain.payment.event;

import java.math.BigDecimal;

/**
 * PaymentProcessedEvent - 表示付款處理完成
 */
public record PaymentProcessedEvent(String paymentId, String orderId, BigDecimal amount) {
}