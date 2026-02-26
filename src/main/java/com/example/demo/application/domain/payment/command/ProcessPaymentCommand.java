package com.example.demo.application.domain.payment.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * ProcessPaymentCommand - 用於執行實際付款動作
 */
public record ProcessPaymentCommand(@TargetAggregateIdentifier String paymentId, String orderId, BigDecimal amount) {
}