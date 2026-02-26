package com.example.demo.application.domain.order.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * CreateOrderCommand - 使用 Record 簡化開發
 */
public record CreateOrderCommand(
    @TargetAggregateIdentifier 
    String orderId, 
    
    BigDecimal amount
) {}