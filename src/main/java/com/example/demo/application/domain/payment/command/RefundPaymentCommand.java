package com.example.demo.application.domain.payment.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * RefundPaymentCommand - 用於執行退款
 */
public record RefundPaymentCommand(@TargetAggregateIdentifier String paymentId, String orderId, BigDecimal amount) {
}