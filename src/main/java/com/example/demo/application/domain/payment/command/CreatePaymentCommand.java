package com.example.demo.application.domain.payment.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * CreatePaymentCommand - 用於建立新的 Payment Aggregate
 */
public record CreatePaymentCommand(
    @TargetAggregateIdentifier String paymentId,
    String orderId,
    BigDecimal amount
) {}	