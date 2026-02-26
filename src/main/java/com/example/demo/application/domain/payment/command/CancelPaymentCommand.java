package com.example.demo.application.domain.payment.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * CancelPaymentCommand - 用於取消付款
 */
public record CancelPaymentCommand(@TargetAggregateIdentifier String paymentId, String orderId) {
}