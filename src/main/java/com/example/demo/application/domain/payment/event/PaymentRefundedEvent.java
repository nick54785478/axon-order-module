package com.example.demo.application.domain.payment.event;

import java.math.BigDecimal;

/**
 * PaymentRefundedEvent - 表示付款已退款
 */
public record PaymentRefundedEvent(String paymentId, String orderId, BigDecimal amount) {
}